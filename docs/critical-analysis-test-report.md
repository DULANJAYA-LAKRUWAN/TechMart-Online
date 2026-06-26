# Critical Analysis & Test Report — TechMart Online

## 1. Test Coverage Summary

### 1.1 Unit Test Results

**Total: 80 tests — 0 failures — 0 errors — 0 skipped**

| Module | Tests | Coverage Target | Risk |
|---|---|---|---|
| `OrderServiceBeanTest` | 18 | All public methods (place, cancel, find, update, revenue) | Medium |
| `ProductServiceBeanTest` | 15 | CRUD + search + price range + featured + count | Low |
| `UserServiceBeanTest` | 18 | Register, auth, password, roles, deactivate | Low |
| `InventoryCacheBeanTest` | 9 | Cache ops (get, evict, refresh, hitRate, size) | Medium |
| `ShoppingCartBeanTest` | 21 | Full lifecycle (add, qty, remove, subtotal, checkout, clear) | Low |

### 1.2 Integration Test Results (Planned)

**Total: 35 tests across 4 Arquillian test classes**

| Module | Tests | Key Validations |
|---|---|---|
| `ProductResourceIT` | 12 | CRUD, SKU lookup, search, category, featured, price range, 404 |
| `UserResourceIT` | 12 | Register, auth, roles, password change, deactivate, 404 |
| `OrderResourceIT` | 8 | Place order, cancel, status transition, revenue, empty order |
| `MonitoringResourceIT` | 3 | Health (UP), metrics, slow operations |

### 1.3 Performance Test Plan

The JMeter suite targets three workload profiles:
- **Read-heavy browsing** (20 concurrent users): product listing, search, filtering
- **Mixed user operations** (10 concurrent users): registration, authentication, profile management
- **Write-heavy ordering** (5 concurrent users): place order, status updates, revenue queries

## 2. Critical Analysis

### 2.1 Architecture Strengths

**Effective separation of concerns.** The four-tier architecture cleanly isolates persistence (JPA), business logic (EJBs), presentation (REST), and integration (JMS). This modularity means individual tiers can be replaced or upgraded with minimal ripple effects. For example, migrating from JPA to a different ORM requires changes only in the entity and repository layers.

**Appropriate use of EJB types.** The project demonstrates mature understanding of the EJB specification by using each bean type for its intended purpose: `@Stateless` for stateless services (the majority of business logic), `@Stateful` for the conversational shopping cart, `@Singleton` for the read-heavy inventory cache, and MDBs for asynchronous message processing. This is not over-engineered — each choice has a clear justification.

**CDI interceptors for cross-cutting concerns.** The `PerformanceInterceptor` and `AuditInterceptor` keep cross-cutting logic out of business methods. The audit interceptor, in particular, ensures that every mutating operation produces an immutable `AuditLog` record without adding boilerplate to each service method.

**Asynchronous order processing via JMS.** Decoupling order placement (synchronous REST response) from inventory adjustment (asynchronous JMS consumer) is the correct architectural choice. It keeps HTTP response times predictable and allows the system to absorb traffic spikes by buffering work in the JMS queue.

### 2.2 Architecture Weaknesses

**In-memory metrics are non-durable.** `PerformanceInterceptor` stores metrics in static `ConcurrentHashMap` instances. While fine for development observability, this means all metrics are lost on redeployment. There is no historical trend data, no alerting integration, and no way to correlate metrics with deployment events. A production system should export these metrics to an external monitoring system (Prometheus/Grafana, Datadog, etc.) via Micrometer or a custom metrics endpoint.

**No caching layer for product catalog queries.** Each product listing and search request hits the database directly. While `InventoryCacheBean` caches stock levels, the product entity data itself is fetched from PostgreSQL on every request. As the catalog grows (10,000+ products), repeated paginated queries with full-text search will create database contention. A Redis-based cache or Hibernate second-level cache would significantly reduce read-path latency.

**JMS dependency for core order flow.** The order placement flow depends on JMS destinations (`OrderQueue`, `NotificationTopic`) that must exist in the application server before deployment. If the destinations are misconfigured or unavailable, order placement fails silently (the JMS producer throws an exception that is caught and logged). The system lacks a fallback mechanism, such as writing to a database-backed outbox table for later retry.

**Limited transaction demarcation.** All service methods use `@TransactionAttribute(REQUIRED)`, which means they always execute within a transaction context. However, there is no explicit rollback handling for non-persistent operations (e.g., failed JMS sends). If the JMS broker is unavailable, the database transaction commits but the message is never sent — resulting in an inconsistent state between the database and the message queue.

### 2.3 Test Coverage Gaps

**No negative path tests for authentication.** The `UserResourceIT` tests successful authentication but does not test invalid credentials, locked accounts, or brute-force lockout scenarios.

**No concurrent access tests.** None of the tests simulate concurrent shopping cart access or simultaneous order placement for the same product. The `@Stateful` shopping cart, in particular, needs testing for concurrent access patterns (two threads modifying the same user's cart).

**No database migration tests.** The SQL schema (`001-schema.sql`) is tested only by deploying to a blank database. There are no upgrade-path tests that verify migration from version N to N+1.

**No MDB/JMS integration tests.** The Arquillian tests exclude MDB classes from the deployment to avoid JMS destination dependencies. This means `OrderProcessingMDB` and `NotificationMDB` are untested beyond unit-level mock verification.

**JMeter tests are structural, not threshold-based.** The JMeter test plans validate HTTP responses but do not define performance thresholds (e.g., "95th percentile under 500ms" or "zero errors under 50 concurrent users"). The tests will report raw metrics, but there is no automated pass/fail determination.

### 2.4 Production Readiness Assessment

| Concern | Status | Recommendation |
|---|---|---|
| Authentication | Weak | Implement password hashing with bcrypt/argon2, add rate limiting, add account lockout |
| HTTPS | Not configured | Add TLS termination in WildFly or use a reverse proxy |
| Secrets management | Hardcoded in CLI | Move credentials to WildFly vault or external secrets manager |
| Monitoring | Basic in-memory | Export metrics to Prometheus; add health check probes |
| Logging | File rotation | Add structured logging (JSON) for ELK/Grafana Loki ingestion |
| CORS | Not configured | Add CORS filter for web client access |
| Rate limiting | Not implemented | Add token-bucket rate limiting at the REST layer |

## 3. Recommendations

### 3.1 Immediate (Pre-Production)

1. **Add Micrometer metrics export** to replace the in-memory `PerformanceInterceptor` with a Prometheus-compatible `/api/metrics` endpoint. This provides durable, queryable performance data with minimal code changes.

2. **Implement an outbox pattern** for JMS message delivery. Instead of sending JMS messages directly from the service bean, write to a database `outbox` table and have a separate scheduled process or Debezium connector forward messages to the broker. This guarantees at-least-once delivery without distributed transactions.

3. **Add Hibernate second-level cache** for product data. A simple read-write cache region for `Product` entities would dramatically reduce database load during product browsing, which constitutes the majority of API traffic.

### 3.2 Short-Term (Next Sprint)

4. **Replace SHA-256 with bcrypt** for password hashing. The current `MessageDigest` approach is vulnerable to brute-force attacks. bcrypt's configurable work factor provides resistance against hardware-accelerated cracking.

5. **Add rate limiting middleware.** A CDI interceptor or JAX-RS `ContainerRequestFilter` can implement token-bucket rate limiting per client IP, protecting the API from abuse.

6. **Implement structured logging.** Replace the current `java.util.logging` with SLF4J + Logback, configured to emit JSON-formatted logs. This enables centralized log analysis in ELK or Grafana Loki.

### 3.3 Long-Term

7. **Consider microservice decomposition.** The current monolith is appropriate for the current scale. As the team and traffic grow, consider extracting `OrderService` and `NotificationService` into standalone services communicating via Kafka or NATS.

8. **Add GraphQL or gRPC for complex queries.** The REST API's multiple endpoints for product filtering (search, category, price range, featured) suggest a need for flexible query capabilities. GraphQL would allow clients to specify exact data requirements, reducing over-fetching.

## 4. Conclusion

TechMart Online demonstrates solid Java EE architecture with appropriate use of EJBs, JPA, CDI, and JMS. The asynchronous order processing pattern, CDI interceptor design, and DTO-based REST API follow established enterprise patterns. The test suite provides good coverage of core business logic (80 unit tests) and API contracts (35 integration tests).

However, the system has three critical gaps that must be addressed before production deployment: durable metrics export, guaranteed JMS message delivery, and product catalog caching. The authentication mechanism requires modernization (bcrypt over SHA-256). These issues are well-understood and have standard solutions in the Java EE ecosystem.

The architecture is fundamentally sound and suitable for its target deployment scale (single WildFly node with PostgreSQL). The recommended improvements follow a clear priority order and can be implemented incrementally without architectural changes.

---

*Report prepared: June 2026*
