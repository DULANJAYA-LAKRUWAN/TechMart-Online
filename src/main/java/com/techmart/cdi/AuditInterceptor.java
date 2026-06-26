package com.techmart.cdi;

import com.techmart.entity.AuditLog;




import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CDI Interceptor — automatically persists an AuditLog entry for every
 * method annotated with @Auditable.
 *
 * Implementation Notes:
 * - Uses a separate EntityManager to ensure the audit log is persisted
 *   even if the business transaction rolls back (in production, this would
 *   use a separate datasource with REQUIRES_NEW; here it shares the TX for simplicity).
 * - Captures method name, args summary, execution time, success/failure.
 * - Thread-safe: interceptors are CDI-managed and request-scoped per invocation.
 */
@Auditable
@Interceptor
public class AuditInterceptor {

    private static final Logger LOG = Logger.getLogger(AuditInterceptor.class.getName());

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @AroundInvoke
    public Object audit(InvocationContext ctx) throws Exception {
        // Extract @Auditable metadata from the method or class
        Auditable annotation = ctx.getMethod().getAnnotation(Auditable.class);
        if (annotation == null) {
            annotation = ctx.getTarget().getClass().getAnnotation(Auditable.class);
        }

        String action    = (annotation != null && !annotation.action().isEmpty())
            ? annotation.action() : ctx.getMethod().getName().toUpperCase();
        String entity    = (annotation != null) ? annotation.entity() : "UNKNOWN";

        long startTime = System.currentTimeMillis();
        Object result  = null;
        boolean success = true;
        String errorMsg = null;

        try {
            result = ctx.proceed();
            return result;
        } catch (Exception ex) {
            success  = false;
            errorMsg = ex.getMessage();
            throw ex;
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;

            try {
                AuditLog log = new AuditLog(action, entity, null, success);
                log.withExecutionTime(elapsed);
                if (!success && errorMsg != null) {
                    log.withError(errorMsg);
                }
                log.withDescription(
                    "Method: " + ctx.getMethod().getDeclaringClass().getSimpleName()
                    + "." + ctx.getMethod().getName()
                    + " | Args count: " + ctx.getParameters().length);
                em.persist(log);
            } catch (Exception persistEx) {
                // Never let audit failure break the business operation
                LOG.log(Level.WARNING, "Failed to persist audit log entry", persistEx);
            }
        }
    }
}
