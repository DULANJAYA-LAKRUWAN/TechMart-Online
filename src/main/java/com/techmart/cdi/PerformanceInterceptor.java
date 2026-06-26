package com.techmart.cdi;

import com.techmart.metrics.MetricsRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CDI Performance Interceptor — measures method execution time for every
 * method annotated with @Monitored.
 *
 * Dual-mode: maintains in-memory counters (for /api/monitor) AND exports to
 * Micrometer/Prometheus (for /api/monitor/prometheus).
 */
@Monitored
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class PerformanceInterceptor {

    private static final Logger LOG = Logger.getLogger(PerformanceInterceptor.class.getName());

    @Inject
    private MetricsRegistry metricsRegistry;

    private static final ConcurrentHashMap<String, AtomicLong> callCounts   = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> totalTimeMs  = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> slowCounts   = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Counter> micrometerCounters  = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Timer>   micrometerTimers    = new ConcurrentHashMap<>();

    @AroundInvoke
    public Object measure(InvocationContext ctx) throws Exception {
        String className  = ctx.getMethod().getDeclaringClass().getSimpleName();
        String methodName = ctx.getMethod().getName();
        String methodKey  = className + "." + methodName;
        String metricName = "techmart." + className + "." + methodName;

        Monitored annotation = ctx.getMethod().getAnnotation(Monitored.class);
        long threshold = (annotation != null) ? annotation.slowThresholdMs() : 500L;

        long start = System.nanoTime();
        try {
            return ctx.proceed();
        } finally {
            long elapsedNanos = System.nanoTime() - start;
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);

            // In-memory counters (for /api/monitor)
            callCounts.computeIfAbsent(methodKey, k -> new AtomicLong()).incrementAndGet();
            totalTimeMs.computeIfAbsent(methodKey, k -> new AtomicLong()).addAndGet(elapsedMs);

            if (elapsedMs > threshold) {
                slowCounts.computeIfAbsent(methodKey, k -> new AtomicLong()).incrementAndGet();
                LOG.log(Level.WARNING,
                    "SLOW OPERATION [{0}] took {1}ms (threshold: {2}ms)",
                    new Object[]{methodKey, elapsedMs, threshold});
            } else {
                LOG.log(Level.FINE,
                    "PERF [{0}] completed in {1}ms",
                    new Object[]{methodKey, elapsedMs});
            }

            // Micrometer metrics (for /api/monitor/prometheus)
            if (metricsRegistry != null) {
                Counter counter = micrometerCounters.computeIfAbsent(methodKey,
                    k -> Counter.builder(metricName + ".count")
                        .tag("class", className)
                        .tag("method", methodName)
                        .description("Invocation count for " + methodKey)
                        .register(metricsRegistry.getRegistry()));
                counter.increment();

                if (elapsedMs > threshold) {
                    Counter slowCounter = micrometerCounters.computeIfAbsent(
                        methodKey + ".slow",
                        k -> Counter.builder(metricName + ".slow")
                            .tag("class", className)
                            .tag("method", methodName)
                            .description("Slow invocation count (>=" + threshold + "ms) for " + methodKey)
                            .register(metricsRegistry.getRegistry()));
                    slowCounter.increment();
                }

                Timer timer = micrometerTimers.computeIfAbsent(methodKey,
                    k -> Timer.builder(metricName + ".duration")
                        .tag("class", className)
                        .tag("method", methodName)
                        .description("Execution time for " + methodKey)
                        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                        .register(metricsRegistry.getRegistry()));
                timer.record(elapsedNanos, TimeUnit.NANOSECONDS);
            }
        }
    }

    public static ConcurrentHashMap<String, AtomicLong> getCallCounts()  { return callCounts;  }
    public static ConcurrentHashMap<String, AtomicLong> getTotalTimeMs() { return totalTimeMs; }
    public static ConcurrentHashMap<String, AtomicLong> getSlowCounts()  { return slowCounts;  }

    public static double getAverageMs(String methodKey) {
        AtomicLong calls = callCounts.get(methodKey);
        AtomicLong total = totalTimeMs.get(methodKey);
        if (calls == null || calls.get() == 0) return 0.0;
        return (double) total.get() / calls.get();
    }
}
