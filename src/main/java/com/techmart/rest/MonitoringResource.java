package com.techmart.rest;

import com.techmart.cdi.PerformanceInterceptor;
import com.techmart.dto.ApiResponseDTO;
import com.techmart.metrics.MetricsRegistry;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Path("/monitor")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MonitoringResource {

    @Inject
    private MetricsRegistry metricsRegistry;

    @GET
    @Path("/metrics")
    public Response getMetrics() {
        ConcurrentHashMap<String, AtomicLong> callCounts  = PerformanceInterceptor.getCallCounts();
        ConcurrentHashMap<String, AtomicLong> totalTimeMs = PerformanceInterceptor.getTotalTimeMs();
        ConcurrentHashMap<String, AtomicLong> slowCounts  = PerformanceInterceptor.getSlowCounts();

        Map<String, Map<String, Object>> metrics = callCounts.keySet().stream()
            .collect(Collectors.toMap(
                k -> k,
                k -> Map.of(
                    "callCount",   callCounts.get(k).get(),
                    "totalTimeMs", totalTimeMs.get(k).get(),
                    "avgMs",       PerformanceInterceptor.getAverageMs(k),
                    "slowCount",   slowCounts.containsKey(k) ? slowCounts.get(k).get() : 0
                )
            ));

        return Response.ok(ApiResponseDTO.success(metrics, "Performance metrics")).build();
    }

    @GET
    @Path("/health")
    public Response healthCheck() {
        return Response.ok(ApiResponseDTO.success(Map.of(
            "status",    "UP",
            "app",       "TechMart Online",
            "version",   "1.0.0"
        ), "Application is healthy")).build();
    }

    @GET
    @Path("/slow")
    public Response getSlowOperations(@QueryParam("threshold") @DefaultValue("500") long threshold) {
        ConcurrentHashMap<String, AtomicLong> slowCounts = PerformanceInterceptor.getSlowCounts();

        Map<String, Long> slowOps = slowCounts.entrySet().stream()
            .filter(e -> e.getValue().get() > 0)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));

        return Response.ok(ApiResponseDTO.success(slowOps, "Slow operations (>=" + threshold + "ms)")).build();
    }

    @GET
    @Path("/prometheus")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPrometheusMetrics() {
        String metrics = metricsRegistry.scrape();
        return Response.ok(metrics).build();
    }
}
