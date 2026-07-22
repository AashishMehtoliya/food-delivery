# Food Delivery Order Management — Implementation Specification

This document is the build spec for a take-home assignment. It should be treated as the
source of truth for scope, design, and acceptance criteria. Where the original prompt was
open-ended, this document makes the scoping decisions explicit — implement to this spec
rather than re-deriving scope from scratch.

Time budget: 48 hours total, including tests, README, and video. Prioritize the concurrency
correctness requirements (Section 7) over feature breadth — they are the technical crux of
the assignment.

---

## 1. Objective

Build a backend-only food delivery order management system: multi-restaurant, multi-city,
with menu management, order placement, order lifecycle tracking, delivery-partner
assignment, and post-delivery ratings. No UI. No deployment/CI. No microservices.

The three things that matter most, in order:
1. **Concurrency correctness** — no overselling stock, no double-assignment of a delivery
   partner, atomic order placement.
2. **Non-blocking async fan-out** — status-change notifications must not sit in the request
   path.
3. **Breadth of core flows**, done cleanly, with tests.

---

## 2. Tech stack

- **Language**: Java 17+
- **Framework**: Spring Boot 3.x
- **Build tool**: Gradle (Groovy or Kotlin DSL — Groovy is fine for a project this size)
- **Persistence**: Spring Data JPA + Hibernate
- **Database**: H2, file-based mode (e.g. `jdbc:h2:file:./data/fooddelivery`) for both dev
  and tests — not in-memory-only, so data survives restarts during manual testing. Keep the
  datasource config in `application.yml` behind a Spring profile (`dev`/`test`) so switching
  to Postgres later is a one-line change, not a rewrite. Note for the README: H2 is being
  used to keep local setup to zero external dependencies (no DB server to install/run),
  which is a reasonable and worth-documenting assumption for a 48-hour take-home.
- **Validation**: `spring-boot-starter-validation` (Jakarta Bean Validation)
- **Security/RBAC**: Spring Security, but **basic** — no OAuth/SSO/MFA. A simple
  authenticated-principal + role-check model (e.g. HTTP Basic or a static bearer-token
  scheme for simplicity) is sufficient. Do not build a full auth server.
- **Testing**: JUnit 5, Mockito, Spring Boot Test (`@SpringBootTest`, `@DataJpaTest`),
  all running against H2 — no Testcontainers needed given H2 is now the primary DB
- **Async**: Spring's `@Async` + `TaskExecutor`, `ApplicationEventPublisher` +
  `@TransactionalEventListener`
- **Migrations**: Flyway (recommended) or `spring.jpa.hibernate.ddl-auto=update` if time
  is short — but document the choice. If using Flyway, keep migration SQL H2-compatible
  (avoid Postgres-only syntax like `SERIAL`/`JSONB`) so the same scripts work if you switch
  DBs later
- **Lombok**: optional, allowed to reduce boilerplate

Do NOT introduce: Kafka, RabbitMQ, Redis, Docker, Kubernetes, multiple services, API
gateways, or any distributed-systems infrastructure. Everything runs as a single Spring
Boot deployable against a single database instance.

---

## 3. Scope

### In scope
- REST APIs for all four roles (admin, restaurant owner, customer, delivery partner)
- Persistence to a relational DB
- Basic role-based access control
- Input validation and structured error handling
- Unit tests (service layer logic) and integration tests (core flows end-to-end,
  including concurrency scenarios)

### Explicitly out of scope — do not build these
- Any frontend/UI
- Deployment, containerization, CI/CD pipelines
- Distributed systems, microservices, message brokers, service discovery
- OAuth/SSO/MFA, JWT refresh-token infra, third-party identity providers
- Production observability (metrics, tracing, alerting, dashboards)
- Real payment gateway integration (mock it — see Section 6)
- Real push notification/SMS/email delivery (mock it — see Section 9)
- Geo-based partner matching (no lat/long, no distance calculation)

---

## 4. Assumptions (already made — do not re-litigate, just implement)

1. Payment is a mocked internal component. `PaymentService.charge(...)` returns a
   deterministic success/failure (e.g. always succeeds, or fails for a specific test
   amount) — no external gateway call.
2. Delivery partner assignment is pool-based: an accepted order is broadcast as an
   "offered" assignment to all available partners; any partner may call an accept
   endpoint; first successful write wins. No nearest-partner / geo matching.
3. Order cancellation is supported only while status is `PLACED`, initiated by the
   customer. Restaurant-side rejection is a separate, already-modeled transition
   (`PLACED → REJECTED`).
4. Async fan-out is in-process (Spring events + a dedicated thread pool), not an external
   broker — consistent with the "no distributed systems" constraint.
5. Tracking is poll-based (`GET /orders/{id}`) — no WebSocket/SSE push to clients.
6. Single relational database instance. No sharding, no read replicas.

Document these (or your own equivalent set, if you deviate) in the submission README.

---

## 5. Roles & RBAC

| Role | Permitted actions |
|---|---|
| `ADMIN` | Create/manage cities, restaurants, delivery partners |
| `RESTAURANT_OWNER` | Manage own restaurant's menu; accept/reject orders placed at own restaurant |
| `CUSTOMER` | Browse restaurants/menu, place orders, track own orders, cancel own `PLACED` orders, rate own delivered orders |
| `DELIVERY_PARTNER` | View offered assignments, accept an assignment, update status on own accepted orders |

Implementation approach: single `User` entity with a `role` enum column. RBAC enforced via
Spring Security method security (`@PreAuthorize("hasRole('ADMIN')")` etc.) at the
controller or service layer. Ownership checks (e.g. "this restaurant owner owns this
restaurant") are business logic in the service layer, not just role checks — a
`RESTAURANT_OWNER` must not be able to modify another restaurant's menu.

---

## 6. Domain model

Implement these entities (adjust field types as needed, but preserve the relationships and
the fields called out as load-bearing for concurrency/correctness):

### `City`
- `id`, `name` (unique)

### `User`
- `id`, `name`, `email` (unique), `passwordHash`, `role` (enum: `ADMIN`,
  `RESTAURANT_OWNER`, `CUSTOMER`, `DELIVERY_PARTNER`)

### `Restaurant`
- `id`, `name`, `city` (FK → City), `owner` (FK → User, role must be `RESTAURANT_OWNER`)

### `MenuItem`
- `id`, `restaurant` (FK), `name`, `price` (BigDecimal), `stockQuantity` (int),
  **`version` (int, `@Version` — JPA optimistic locking column)**, `available` (boolean)

### `DeliveryPartner`
- `id`, `user` (FK → User, role `DELIVERY_PARTNER`), `availabilityStatus` (enum:
  `AVAILABLE`, `BUSY`)

### `Order`
- `id`, `customer` (FK → User), `restaurant` (FK), `status` (enum — see Section 7 for the
  state machine), `totalAmount` (BigDecimal), `createdAt`, `updatedAt`

### `OrderItem`
- `id`, `order` (FK), `menuItem` (FK), `quantity` (int), `priceAtOrderTime` (BigDecimal —
  snapshot, do not read live `MenuItem.price` for historical orders)

### `Payment`
- `id`, `order` (FK, one-to-one), `amount`, `status` (enum: `SUCCESS`, `FAILED`), `method`
  (mocked, e.g. always `"MOCK_WALLET"`)

### `DeliveryAssignment`
- `id`, `order` (FK, one-to-one), `partner` (FK → DeliveryPartner, nullable until
  accepted), `status` (enum: `OFFERED`, `ACCEPTED`, `EXPIRED`), `offeredAt`, `acceptedAt`

### `Rating`
- `id`, `order` (FK), `customer` (FK), `restaurantRating` (1–5), `partnerRating` (1–5),
  `comment` (nullable), `createdAt`

---

## 7. Order lifecycle (state machine)

```
PLACED → ACCEPTED → PREPARING → OUT_FOR_DELIVERY → DELIVERED
   |         |
   |         └─→ (assignment offered to partner pool on ACCEPTED)
   └─→ REJECTED (by restaurant)
   └─→ CANCELLED (by customer, only from PLACED)
```

Implement this as an explicit enum + a validated transition method (e.g.
`Order.transitionTo(newStatus)` that throws `InvalidStateTransitionException` for illegal
jumps — do not allow arbitrary status writes from the API). Every transition endpoint must
check the current status server-side before applying the next one, even if the client sends
a "valid-looking" request.

---

## 8. API specification

All endpoints require an authenticated principal except the browse endpoints. Apply
`@PreAuthorize` per the RBAC table in Section 5. All request bodies must be validated
(`@Valid` + Bean Validation annotations); return `400` with a structured error body on
validation failure.

### Admin
| Method | Path | Notes |
|---|---|---|
| POST | `/admin/cities` | name required, unique |
| POST | `/admin/restaurants` | cityId, name, ownerId (must be a RESTAURANT_OWNER user) |
| POST | `/admin/delivery-partners` | userId (must be a DELIVERY_PARTNER user) |
| GET | `/admin/restaurants`, `/admin/delivery-partners` | listing, pagination optional |

### Restaurant owner
| Method | Path | Notes |
|---|---|---|
| POST | `/restaurants/{id}/menu-items` | ownership check |
| PATCH | `/restaurants/{id}/menu-items/{itemId}` | partial update (price/stock/available) |
| GET | `/restaurants/{id}/orders?status=PLACED` | owner's action queue |
| POST | `/orders/{id}/accept` | must be in `PLACED`; triggers assignment broadcast |
| POST | `/orders/{id}/reject` | must be in `PLACED`; triggers refund path (mocked) |

### Customer
| Method | Path | Notes |
|---|---|---|
| GET | `/cities/{id}/restaurants` | public read |
| GET | `/restaurants/{id}/menu` | public read, only `available=true` items shown |
| POST | `/orders` | body: `restaurantId`, `items: [{menuItemId, quantity}]` — see Section 9 for atomicity requirement |
| GET | `/orders/{id}` | ownership check — customer can only view own orders |
| POST | `/orders/{id}/cancel` | must be in `PLACED` |
| POST | `/orders/{id}/rating` | must be in `DELIVERED`, one rating per order |

### Delivery partner
| Method | Path | Notes |
|---|---|---|
| GET | `/delivery-partners/{id}/assignments?status=OFFERED` | pool of open assignments |
| POST | `/orders/{id}/assignments/accept` | race-resolution endpoint — see Section 10 |
| PATCH | `/orders/{id}/status` | body: `{status}`; only `OUT_FOR_DELIVERY` or `DELIVERED`, only by the accepted partner |

Error responses should be a consistent JSON shape, e.g.:
```json
{ "timestamp": "...", "status": 409, "error": "STOCK_UNAVAILABLE", "message": "..." }
```
Use `@ControllerAdvice`/`@ExceptionHandler` for a global exception handler mapping domain
exceptions (e.g. `InsufficientStockException`, `InvalidStateTransitionException`,
`AssignmentAlreadyAcceptedException`) to appropriate HTTP status codes (409 for conflicts,
404 for not found, 403 for authorization failures, 400 for validation).

---

## 9. Atomic order placement (critical requirement)

`POST /orders` must, in a **single `@Transactional` service method**:
1. Validate all requested `MenuItem`s exist, belong to the given restaurant, and are
   `available`.
2. For each item, atomically decrement stock using an optimistic-locking update, e.g.:
   ```sql
   UPDATE menu_item SET stock_quantity = stock_quantity - :qty, version = version + 1
   WHERE id = :itemId AND version = :version AND stock_quantity >= :qty
   ```
   (or rely on JPA's `@Version` field + a plain `save()`, letting Hibernate throw
   `OptimisticLockException` on conflict — either approach is acceptable, but the
   conflict-detection mechanism must be explicit and tested)
3. If any item fails the stock check, roll back the entire transaction and return `409
   INSUFFICIENT_STOCK` — no partial order should ever be persisted.
4. Create the `Order` and `OrderItem` rows.
5. Call the mocked `PaymentService` and create the `Payment` row. If payment "fails," roll
   back everything (stock decrements included).
6. Only after successful commit, publish an internal domain event (see Section 11) — do
   **not** call the notification logic inside step 1–5's transaction.

On an optimistic lock conflict, implement a **bounded retry** (e.g. up to 3 attempts) before
giving up and returning a clean "temporarily unavailable, please retry" response to the
client — do not let `OptimisticLockException` bubble up as a raw 500.

---

## 10. Delivery partner assignment (critical requirement)

When an order transitions to `ACCEPTED` (restaurant accepts), create one
`DeliveryAssignment` row with `status=OFFERED` (visible to all available partners — no
per-partner rows needed, a single shared assignment record is enough given pool-based
accept).

`POST /orders/{id}/assignments/accept` must resolve the race with a single atomic
conditional update, not a read-then-write:
```sql
UPDATE delivery_assignment
SET partner_id = :partnerId, status = 'ACCEPTED', accepted_at = now()
WHERE order_id = :orderId AND status = 'OFFERED'
```
Check the affected row count. If `1`, the caller won — proceed to mark the partner `BUSY`
and update `Order` status if applicable. If `0`, someone else already accepted — return
`409 ASSIGNMENT_ALREADY_TAKEN`. Do not implement this as
`findById → check status in Java → save()` — that reintroduces the race you're supposed to
close.

---

## 11. Async notification fan-out (critical requirement)

Requirement: any order status change notifies customer, restaurant, and delivery partner
without adding latency to the triggering request.

Implementation:
1. Define a `OrderStatusChangedEvent` (or similar) carrying `orderId`, `oldStatus`,
   `newStatus`.
2. Publish it via `ApplicationEventPublisher` from inside the service method that performs
   the status change — but the actual notification work must run **after** the transaction
   commits, via:
   ```java
   @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
   @Async
   public void onOrderStatusChanged(OrderStatusChangedEvent event) { }
   ```
3. Configure a dedicated, **bounded** `ThreadPoolTaskExecutor` for this (not the default
   `SimpleAsyncTaskExecutor`, which is unbounded) — set a core/max pool size and a bounded
   queue, with a `CallerRunsPolicy` (or logged rejection) so a burst degrades rather than
   OOMs.
4. The notification "delivery" itself can be mocked — e.g. log the notification or persist
   a `Notification` record — since real push/SMS/email integration is out of scope. What's
   being tested here is the **async, non-blocking, after-commit** wiring, not the delivery
   channel.

---

## 12. Validation & error handling

- Use Bean Validation (`@NotNull`, `@Min`, `@Email`, etc.) on all request DTOs — do not
  accept entity objects directly as request bodies.
- Centralize exception → HTTP status mapping in a single `@RestControllerAdvice`.
- Define domain-specific exceptions rather than generic `RuntimeException`:
  `InsufficientStockException`, `InvalidStateTransitionException`,
  `AssignmentAlreadyAcceptedException`, `UnauthorizedActionException`,
  `ResourceNotFoundException`.
- Never let a raw stack trace leak into an API response body.

---

## 13. Testing requirements

### Unit tests (service layer, Mockito)
- Order placement: happy path, insufficient stock, payment failure rollback
- State transition validation: illegal transitions rejected
- Assignment acceptance: winner path, already-taken path
- Rating: only allowed post-delivery, one per order

### Integration tests (`@SpringBootTest`, real transactions against H2)
- Full order placement flow end-to-end (menu browse → place order → verify stock, order,
  payment rows all committed together)
- **Concurrency test**: use an `ExecutorService` with N threads (e.g. 10) all calling
  `POST /orders` against a `MenuItem` with `stockQuantity=5` simultaneously; assert exactly
  5 succeed and the rest get `409 INSUFFICIENT_STOCK`, and final `stockQuantity == 0` (never
  negative).
- **Concurrency test**: multiple simulated partners calling accept on the same assignment
  concurrently; assert exactly one succeeds.
- RBAC test: a `CUSTOMER` token hitting an `ADMIN`-only endpoint gets `403`.
- Async fan-out test: assert the notification handler runs on a different thread than the
  request thread, and that a rolled-back order produces no notification.

These concurrency tests are the single most important artifact in the submission — they're
the direct proof that Sections 9 and 10 actually work under load, not just in the happy
path.

---

## 14. Suggested package structure

```
com.example.fooddelivery
├── config          (SecurityConfig, AsyncConfig/ThreadPoolTaskExecutor, FlywayConfig)
├── controller       (one per role/resource area)
├── dto              (request/response DTOs, separate from entities)
├── entity
├── enums            (OrderStatus, AssignmentStatus, Role, etc.)
├── event            (OrderStatusChangedEvent + listener)
├── exception         (domain exceptions + @RestControllerAdvice)
├── repository        (Spring Data JPA interfaces)
├── service           (business logic, @Transactional boundaries live here)
└── security          (basic auth/RBAC wiring)
```

---

## 15. Suggested build order (for incremental commits)

1. Project scaffold, entities, repositories, Flyway migrations / schema
2. Admin APIs (cities, restaurants, delivery partners) + basic RBAC wiring
3. Restaurant owner APIs (menu management)
4. Customer browse APIs
5. Order placement with atomicity + optimistic locking (Section 9) + its tests
6. Restaurant accept/reject + assignment offer creation
7. Delivery partner assignment accept (Section 10) + its concurrency test
8. Delivery status updates + order lifecycle enforcement
9. Async notification fan-out (Section 11) + its test
10. Ratings
11. Global exception handling polish, validation pass, README with documented assumptions

Commit at each numbered step (or smaller) — the assignment explicitly wants visible
incremental history, not one final commit.

---

## 16. Submission checklist (do not skip)

- [ ] GitHub repo with multiple incremental commits
- [ ] `README.md` documenting every assumption made (start from Section 4 of this doc,
  note any deviations)
- [ ] `Claude.md`/`Agents.md` describing the AI workflow used during development
- [ ] All raw files used during development included in the repo
- [ ] Unit + integration tests for all core flows, including the two concurrency tests in
  Section 13
- [ ] ≤10-minute video covering: approach, tech stack rationale, AI workflow, testing
  approach