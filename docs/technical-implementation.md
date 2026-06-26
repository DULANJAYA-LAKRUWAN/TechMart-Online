# TechMart Online — Technical Implementation Document

## 1. Architecture Overview

TechMart Online is an enterprise-grade e-commerce platform built on the Java EE 8 (Jakarta EE) specification. It follows a multi-tier architecture with clear separation of concerns across four layers: persistence, business, presentation, and integration.

**Technology Stack:**
- **Language:** Java 21
- **Platform:** Java EE 8 (javax.\* namespace), deployable on WildFly 34+ / Payara 5+
- **Build:** Maven 3.9.16 with profile-based test segregation
- **Database:** PostgreSQL 14+ with Hibernate 5.x ORM
- **Messaging:** Jakarta JMS with ActiveMQ Artemis (embedded in WildFly)
- **CDI:** Weld (WildFly's CDI implementation)
- **REST:** JAX-RS 2.1 with automatic resource discovery

The application is packaged as a single WAR (`techmart-online.war`) deployed to WildFly. All infrastructural concerns — datasource pooling, JMS destinations, logging — are configured via the WildFly management CLI rather than embedded in the application, following the principle of externalized configuration.

```
┌──────────────────────────────────────────────────────────┐
│                    Presentation Tier                      │
│  JAX-RS REST API (@ApplicationPath("/api"))              │
│  JSON via DTOs, ApiResponseDTO envelope                  │
├──────────────────────────────────────────────────────────┤
│                    Business Tier                          │
│  @Stateless EJBs (ProductService, UserService,           │
│    OrderService, NotificationService)                    │
│  @Stateful EJB (ShoppingCart)                            │
│  @Singleton EJB (InventoryCache, Configuration)          │
│  @Asynchronous + MDBs (order processing, notifications)  │
│  CDI @Interceptors (PerformanceInterceptor,              │
│    AuditInterceptor)                                     │
│  CDI Events (OrderPlacedEvent → OrderEventObserver)      │
├──────────────────────────────────────────────────────────┤
│                    Persistence Tier                       │
│  JPA Entities, @PersistenceContext EntityManager         │
│  JPQL NamedQueries, @SequenceGenerator                   │
│  PostgreSQL (via java:/jdbc/TechMartDS)                  │
├──────────────────────────────────────────────────────────┤
│                    Integration Tier                       │
│  JMS Queue (OrderQueue) for order processing             │
│  JMS Topic (NotificationTopic) for event broadcast       │
└──────────────────────────────────────────────────────────┘
```

## 2. Persistence Layer

### 2.1 Entity Model

The domain model comprises 10 entities mapped to PostgreSQL via JPA annotations. The schema design emphasizes referential integrity with foreign key constraints, cascading operations where appropriate, and indexed columns for query performance.

**Core Entities and Relationships:**
- **User ↔ Role:** Many-to-many via `user_roles` join table. Supports fine-grained RBAC.
- **Product ↔ Category:** Many-to-one with `@JoinColumn`. Products cascade persist from Category.
- **Product → Inventory → Warehouse:** Products have multiple inventory records across warehouses. The `product_id + warehouse_id` unique constraint prevents duplicate stock entries.
- **Order → OrderItem → Product:** One-to-many with cascade from Order. Each item references a product snapshot via FK.
- **AuditLog:** Standalone entity populated by the `AuditInterceptor` for immutable audit trail.
- **Notification:** Standalone entity for persisted notification records, populated by `NotificationMDB`.

### 2.2 Schema Design Decisions

| Decision | Rationale |
|---|---|
| `VARCHAR(60)` for password | Accommodates SHA-256 hex digest (64 chars) with buffer |
| `DECIMAL(10,2)` for monetary values | Precision-safe for financial calculations |
| `TEXT` for descriptions | Product specs can exceed VARCHAR limits |
| Soft-delete (`active BOOLEAN`) on Product/User | Preserves referential integrity on historical orders |
| `@SequenceGenerator` over `IDENTITY` | Batch insert optimization in Hibernate 5 |
| Index on `(status, created_at)` | Accelerates order listing/filtering queries |
| Index on `order_items(order_id)` | Faster join performance for order detail views |

### 2.3 JPA Configuration

`persistence.xml` is configured for the `TechMartPU` unit with Hibernate 5.x as the provider. Key settings:
- `hibernate.dialect=org.hibernate.dialect.PostgreSQL10Dialect` — optimized for PostgreSQL 10+
- `hibernate.jdbc.batch_size=20` — reduces round trips for bulk operations
- `javax.persistence.schema-generation.database.action=validate` — production safety; schema is managed via `001-schema.sql`
- Connection pooling delegated to WildFly's `TechMartDS` datasource (configured via CLI)

## 3. Business Layer

### 3.1 EJB Design

The business tier uses all four EJB types to address different concerns:

**@Stateless (ProductServiceBean, UserServiceBean, OrderServiceBean, NotificationServiceBean)**
Stateless session beans handle the majority of business logic. They are pooled by the container and do not retain client state, making them naturally thread-safe and suitable for high-concurrency scenarios. Each bean implements a local business interface (e.g., `ProductService`) used by REST resources via `@EJB` injection.

**@Stateful (ShoppingCartBean)**
The shopping cart is a conversational scoped bean with `@StatefulTimeout(30, MINUTES)` and `@Remove` on the checkout method. Each client session gets its own cart instance. The timeout ensures idle carts are garbage-collected by the container, preventing memory leaks.

**@Singleton (InventoryCacheBean, ConfigurationBean)**
`InventoryCacheBean` is a `@Startup` singleton that loads stock levels into a `ConcurrentHashMap` at deployment time. A `@Schedule(hour="*/1")` method refreshes the cache hourly, while individual entries are evicted after write operations. This avoids repeated database round-trips for stock checks during order placement.

**@MessageDriven (OrderProcessingMDB, NotificationMDB)**
MDBs listen on JMS destinations for asynchronous processing. `OrderProcessingMDB` consumes `OrderQueue` messages to update inventory asynchronously after order placement. `NotificationMDB` subscribes to `NotificationTopic` to send notifications.

### 3.2 CDI Interceptors

Two interceptors provide cross-cutting concerns:

**PerformanceInterceptor (@Monitored)**
Applied at the class level on all service beans. Records method invocation count, total execution time, and slow-operation count (threshold: 500ms) in static `ConcurrentHashMap` instances. Exposed via `MonitoringResource.getMetrics()` and `getSlowOperations()` — enabling runtime performance observation without APM tooling.

**AuditInterceptor (@Auditable)**
Applied on mutating service methods (CREATE_PRODUCT, UPDATE_USER, PLACE_ORDER, etc.). Persists an `AuditLog` record with the action type, entity name, entity ID, user, and timestamp. This provides an immutable audit trail for compliance requirements.

### 3.3 Asynchronous Processing

Order placement triggers a chain of asynchronous operations:
1. The `OrderServiceBean.placeOrder()` method (synchronous) validates items, reserves inventory, creates the order record, and fires a CDI `OrderPlacedEvent`.
2. The `OrderEventObserver` (asynchronous via `@Observes(during=AFTER_SUCCESS)`) sends a JMS message to `OrderQueue` via `OrderQueueProducer`.
3. `OrderProcessingMDB` consumes the queue message and updates inventory quantities asynchronously.
4. `NotificationMDB` receives the topic broadcast and persists a `Notification` record.

This decouples the HTTP request lifecycle from resource-intensive background processing, keeping response times low.

### 3.4 Caching Strategy

`InventoryCacheBean` maintains an in-memory cache of `productId → availableStock` using `ConcurrentHashMap`:
- **Population:** On `@PostConstruct` and hourly via `@Schedule`
- **Read:** `getAvailableStock(productId)` returns cached value (cache hit) or queries DB (cache miss)
- **Write-through:** `evict(productId)` called after successful order placement
- **Monitoring:** `getHitRate()` reports cache effectiveness

This caching layer is critical for the product browsing and order placement flows, where stock availability must be checked frequently.

## 4. Presentation Layer

### 4.1 REST API Design

The API follows resource-oriented design with consistent response envelopes:

| Resource | Base Path | Endpoints |
|---|---|---|
| Products | `/api/products` | 11 endpoints (CRUD, search, filter, count) |
| Users | `/api/users` | 10 endpoints (register, auth, profile, roles) |
| Orders | `/api/orders` | 10 endpoints (place, list, status, revenue) |
| Monitoring | `/api/monitor` | 3 endpoints (health, metrics, slow ops) |
| Inventory | `/api/inventory` | Stock queries |
| Notifications | `/api/notifications` | User notification list |

Every response uses `ApiResponseDTO<T>` envelope: `{ success, message, data, errorCode, timestamp, totalCount }`. Error responses include a machine-readable `errorCode` for client-side handling.

### 4.2 API Design Decisions

- **DTOs over entities**: REST endpoints never expose JPA entities directly. DTOs decouple the API contract from the persistence model, preventing lazy-loading exceptions and over-sharing of internal state.
- **Map<String, String> for creation requests**: POST endpoints accept flat string maps rather than nested DTOs. This simplifies client integration while server-side validation (Bean Validation) ensures data integrity.
- **Exception mappers**: `GlobalExceptionMapper` catches unhandled exceptions and returns consistent error responses with HTTP status codes, preventing stack trace leaks.

## 5. Testing Strategy

### 5.1 Unit Tests (80 tests)

Five JUnit 5 + Mockito 5 test files cover all service beans:

| Test Class | Tests | Mock Strategy |
|---|---|---|
| `ProductServiceBeanTest` | 15 | Mock EntityManager, TypedQuery |
| `UserServiceBeanTest` | 18 | Mock EntityManager, password hashing |
| `OrderServiceBeanTest` | 17 | Mock EntityManager, JMS producer, CDI events |
| `InventoryCacheBeanTest` | 9 | Mock ProductService for cache population |
| `ShoppingCartBeanTest` | 21 | Pure state-based (no mocking needed) |

Each mock uses separate `TypedQuery` instances to avoid stubbing collisions. The tests run via `mvn test` (Surefire, active by default).

### 5.2 Integration Tests (35 tests)

Arquillian 1.7 + REST Assured 5.4 test the deployed WAR against a live WildFly instance:

| Test Class | Tests | Coverage |
|---|---|---|
| `ProductResourceIT` | 12 | All product CRUD + filter + search endpoints |
| `UserResourceIT` | 12 | Registration, auth, role management |
| `OrderResourceIT` | 8 | Order lifecycle, status transitions, revenue |
| `MonitoringResourceIT` | 3 | Health, metrics, slow operations |

Tests use `@RunAsClient` mode — each deploys its own WAR via `ShrinkWrap`, then makes HTTP assertions via REST Assured. Run via `mvn verify -Pintegration-tests`.

### 5.3 Performance Tests

JMeter 5.x test plans simulate production traffic:
- **API Benchmark** (4 thread groups, 37 threads total): Concurrent product browsing (20 threads), user operations (10 threads), order placement (5 threads), and health monitoring (2 threads). Each sampler validates response success via JSON path assertions. Think times (100ms–3s) model realistic user behavior.
- **E2E Flow** (3 threads, 5 iterations each): Full shopping journey — health check → register → login → browse → place order → confirm → verify.

Results are captured as JTL files and HTML reports for analysis.

## 6. Deployment & DevOps

### 6.1 WildFly Configuration

`setup-wildfly.cli` automates server configuration:
1. **Datasource** (`TechMartDS`): PostgreSQL connection pool (XA-compatible, min 5 / max 20 connections, connection validation with background checks)
2. **JMS Destinations**: `OrderQueue` (queue, persistent) and `NotificationTopic` (topic, non-persistent)
3. **Logging**: Separate `techmart-logger` with daily rotation and 90-day retention
4. **System Properties**: Application-scoped config values

### 6.2 Build Profiles

| Profile | Activation | Scope | Command |
|---|---|---|---|
| `unit-tests` | Default (activeByDefault) | Unit tests | `mvn test` |
| `integration-tests` | Manual (-P) | Integration tests | `mvn verify -Pintegration-tests` |

### 6.3 Deployment Pipeline

The Windows deployment script (`deploy.ps1`) handles the full lifecycle:
1. Builds the WAR with Maven
2. Copies to WildFly's `standalone/deployments/`
3. Uses WildFly's marker-file deployment scanner for automatic deployment

## 7. Key Design Decisions & Trade-offs

**Java EE 8 vs Jakarta EE:** The project uses `javax.*` namespace for maximum WildFly 34 compatibility. The `primefaces` dependency uses the `jakarta` classifier, demonstrating dual-namespace coexistence.

**Separate beans.xml in META-INF and WEB-INF:** CDI interceptors must be declared in `beans.xml`. Placing it in both locations ensures interoperability across different container configurations (some scan `WEB-INF/`, others `META-INF/`).

**Schema validation over create-drop:** The persistence unit uses `validate` mode. Schema migration is handled via SQL scripts, not Hibernate auto-generation. This provides DBA-friendly, reviewable schema changes in production.

**Map<String, String> request bodies:** While less type-safe than dedicated DTOs, flat string maps reduce API surface complexity and avoid serialization issues with nested JSON objects in JAX-RS.

**In-memory PerformanceInterceptor storage:** Metrics are stored in static `ConcurrentHashMap` instances rather than a database or external metrics system. This provides zero-infrastructure observability but means metrics reset on redeployment. For production, Micrometer/Prometheus export is recommended.

**JMS destination creation via CLI:** Rather than relying on `@JMSDestinationDefinition` (which may not trigger correctly in all containers), JMS destinations are created explicitly in the WildFly CLI script. This ensures consistent availability across deployments.

**Stateless session beans for services:** While @Stateless beans are pooled and thread-safe, they require careful handling of persistence context (EXTENDED vs TRANSACTION). The project uses `@TransactionAttribute(REQUIRED)` on all service methods to ensure consistent transaction boundaries.

---

*Document version: 1.0 — June 2026*
