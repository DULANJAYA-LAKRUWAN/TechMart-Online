package com.techmart.cdi;

import javax.interceptor.InterceptorBinding;
import java.lang.annotation.*;

/**
 * CDI Interceptor Binding annotation — marks EJB methods for performance monitoring.
 *
 * Usage:
 *   @Monitored
 *   public List<ProductDTO> findAll(...) { ... }
 *
 * The PerformanceInterceptor will measure execution time and log slow operations.
 */
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Monitored {

    /** Threshold in milliseconds — log a WARNING if exceeded */
    long slowThresholdMs() default 500L;
}
