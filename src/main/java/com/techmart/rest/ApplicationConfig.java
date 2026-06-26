package com.techmart.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application configuration.
 * All REST endpoints are accessible under /api/* path.
 *
 * Design: Using @ApplicationPath instead of web.xml servlet mapping
 * keeps configuration code-centric and avoids XML bloat.
 */
@ApplicationPath("/api")
public class ApplicationConfig extends Application {
    // CDI + scanning handles discovery — no need to list resources manually.
}
