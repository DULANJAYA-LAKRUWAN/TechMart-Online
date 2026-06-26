package com.techmart.cdi;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CDI Performance Interceptor — measures method execution time for every
 * method annotated with @Monitored.
 *
 * Features:
 * - Logs WARNING if execution time exceeds the configured slowThresholdMs.
 * - Maintains in-memory call counters per method for the /api/monitor endpoint.
 * - Thread-safe using ConcurrentHashMap + AtomicLong.
 *
 * Design Note: In production, metrics would be exported to Prometheus/Micrometer.
 * For this assessment, they are stored in memory and exposed via MonitoringResource.
 */
@Monitored
@Interceptor
public class PerformanceInterceptor {

    private static final Logger LOG = Logger.getLogger(PerformanceInterceptor.class.getName());

    // Shared static registry — survives across interceptor instances
    private static final ConcurrentHashMap<String, AtomicLong> callCounts   = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> totalTimeMs  = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> slowCounts   = new ConcurrentHashMap<>();

    @AroundInvoke
    public Object measure(InvocationContext ctx) throws Exception {
        String methodKey = ctx.getMethod().getDeclaringClass().getSimpleName()
            + "." + ctx.getMethod().getName();

        // Get slow threshold from annotation
        Monitored annotation = ctx.getMethod().getAnnotation(Monitored.class);
        long threshold = (annotation != null) ? annotation.slowThresholdMs() : 500L;

        long start = System.currentTimeMillis();
        try {
            return ctx.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;

            // Update counters
            callCounts.computeIfAbsent(methodKey, k -> new AtomicLong()).incrementAndGet();
            totalTimeMs.computeIfAbsent(methodKey, k -> new AtomicLong()).addAndGet(elapsed);

            if (elapsed > threshold) {
                slowCounts.computeIfAbsent(methodKey, k -> new AtomicLong()).incrementAndGet();
                LOG.log(Level.WARNING,
                    "SLOW OPERATION [{0}] took {1}ms (threshold: {2}ms)",
                    new Object[]{methodKey, elapsed, threshold});
            } else {
                LOG.log(Level.FINE,
                    "PERF [{0}] completed in {1}ms",
                    new Object[]{methodKey, elapsed});
            }
        }
    }

    // ── Static accessors for MonitoringResource ──────────────────────────────

    public static ConcurrentHashMap<String, AtomicLong> getCallCounts()  { return callCounts;  }
    public static ConcurrentHashMap<String, AtomicLong> getTotalTimeMs() { return totalTimeMs; }
    public static ConcurrentHashMap<String, AtomicLong> getSlowCounts()  { return slowCounts;  }

    /** Returns average response time in ms for a given method, or 0 if never called */
    public static double getAverageMs(String methodKey) {
        AtomicLong calls = callCounts.get(methodKey);
        AtomicLong total = totalTimeMs.get(methodKey);
        if (calls == null || calls.get() == 0) return 0.0;
        return (double) total.get() / calls.get();
    }
}
