package com.techmart.metrics;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.logging.Logger;

@Singleton
@Startup
public class MetricsRegistry {

    private static final Logger LOG = Logger.getLogger(MetricsRegistry.class.getName());

    private PrometheusMeterRegistry registry;

    @PostConstruct
    public void init() {
        this.registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        this.registry.config().commonTags("app", "techmart-online", "version", "1.0.0");
        LOG.info("MetricsRegistry: PrometheusMeterRegistry initialized");
    }

    public PrometheusMeterRegistry getRegistry() {
        return registry;
    }

    public String scrape() {
        return registry.scrape();
    }
}
