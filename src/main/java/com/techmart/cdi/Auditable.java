package com.techmart.cdi;

import javax.interceptor.InterceptorBinding;
import java.lang.annotation.*;

/**
 * CDI Interceptor Binding annotation — marks EJB methods for audit logging.
 *
 * Usage:
 *   @Auditable(action = "CREATE_PRODUCT", entity = "Product")
 *   public ProductDTO createProduct(...) { ... }
 *
 * The AuditInterceptor will automatically capture the call and persist an AuditLog entry.
 */
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /** Human-readable action label stored in audit_logs.action */
    String action() default "";

    /** Entity type being acted upon */
    String entity() default "";
}
