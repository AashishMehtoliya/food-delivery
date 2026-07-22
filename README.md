# Food Delivery Order Management

Backend-only food delivery order management system: multi-restaurant, multi-city, with
menu management, order placement, order lifecycle tracking, delivery-partner assignment,
and post-delivery ratings. Built to the spec in [`IMPLEMENTATION_SPECS.md`](IMPLEMENTATION_SPECS.md);
this README documents the assumptions and deviations made while implementing it.

## Tech stack

Java 17, Spring Boot 3.5.16 (latest 3.x release — Spring Initializr in this environment
only serves 4.x now, so the project was hand-assembled with Gradle rather than generated),
Spring Data JPA/Hibernate, Spring Security (HTTP Basic), H2 file-based database, Flyway,
Lombok. See Section 2 of the spec for the full rationale.

## Running it

```
./gradlew bootRun          # dev profile, H2 file at ./data/fooddelivery
./gradlew test             # test profile, H2 file at ./data/fooddelivery-test
```

The H2 console is available at `/h2-console` when running with the `dev` profile.

A bootstrap `ADMIN` account is seeded via Flyway migration (`V2__seed_admin_user.sql`),
since there is no other way to reach any admin-only endpoint on a fresh database:

```
email: admin@fooddelivery.com
password: admin123
```

Dev/test credentials only — not meant to survive past this take-home.

## Assumptions carried over from the spec (Section 4)

1. Payment is mocked (`PaymentService.charge`) — succeeds unless the order total is
   exactly `13.13`, which deterministically fails so the rollback path is testable.
2. Delivery assignment is pool-based: one shared `DeliveryAssignment` row per order,
   `OFFERED` to all partners, first successful accept wins.
3. Cancellation is customer-initiated and only allowed from `PLACED`.
4. Async fan-out is in-process (Spring events + a dedicated thread pool), not a broker.
5. Tracking is poll-based (`GET /orders/{id}`), no push.
6. Single relational database instance.

## Deviations and scoping decisions made during implementation

The spec is detailed but left a few real gaps — places where an endpoint or entity implied
by one part of the spec was never actually defined elsewhere. Rather than re-litigating
scope, each was resolved with the simplest change consistent with the rest of the spec,
documented here:

- **User creation.** Admin endpoints reference existing `RESTAURANT_OWNER` and
  `DELIVERY_PARTNER` users by ID (e.g. `POST /admin/restaurants` takes an `ownerId`), but
  no endpoint anywhere creates a `User`. Added a public `POST /auth/register` (name, email,
  password, role) that accepts any role **except** `ADMIN` — admins can only be created via
  the seeded bootstrap account or, in a real deployment, directly against the database.
- **`ACCEPTED -> PREPARING` has no endpoint.** The delivery-partner API only exposes
  `PATCH /orders/{id}/status` for `OUT_FOR_DELIVERY` or `DELIVERED`; nothing moves an order
  from `ACCEPTED` to `PREPARING`. Treated a delivery partner accepting the assignment
  (`POST /orders/{id}/assignments/accept`) as that trigger — once a partner is lined up,
  kitchen prep begins.
- **Nothing frees a delivery partner after acceptance.** A partner flips to `BUSY` on
  assignment acceptance, but no endpoint sets them back to `AVAILABLE`. Order reaching
  `DELIVERED` does that as a side effect, since it's the natural end of the partner's
  involvement.
- **Cancellation refund.** Section 8 only mentions a mocked refund for restaurant rejection,
  not customer cancellation — but payment is captured at placement time either way, so
  leaving cancellation un-refunded would be an inconsistent, clearly-wrong business flow.
  Cancellation goes through the same mocked `PaymentService.refund(...)` path as rejection.
- **Optimistic-lock retry bound.** Section 9 suggests "up to 3 attempts" as an example.
  Under the 10-thread/stock=5 concurrency test, 3 attempts let an unlucky thread exhaust
  its retries via repeated version conflicts even though stock was still available,
  understating the number of legitimate winners without ever overselling. Raised to 10 —
  still strictly bounded, just sized to the contention level the required test exercises.
- **Notification "delivery."** Section 11 allows logging or persisting a `Notification`
  record, but Section 6's domain model has no such entity. Rather than deviating from the
  documented schema, notifications are logged via SLF4J and also recorded in an in-memory
  `NotificationRegistry` (order id, old/new status, handling thread, timestamp) — enough to
  prove the async, after-commit wiring works without a migration.
- **Assignment pool ownership.** `GET /delivery-partners/{id}/assignments` is scoped so a
  partner can only query their own `{id}` (checked against the authenticated principal),
  even though the underlying `OFFERED` pool itself is global and not partner-specific —
  consistent with the ownership-check pattern used everywhere else in the API.

## Design notes

- **Order state machine**: `Order.transitionTo(OrderStatus)` (Section 7) is the single
  source of truth for legal transitions; every status-changing endpoint calls it rather
  than writing status directly, so illegal jumps are rejected the same way everywhere.
- **Stock decrement (Section 9)**: JPA optimistic locking (`MenuItem.version` +
  `saveAndFlush`), not a hand-written conditional SQL `UPDATE`. The conflict-detection
  mechanism is still explicit (`OptimisticLockingFailureException`, caught by a bounded
  retry loop) and is covered by both unit and concurrency tests.
- **Assignment acceptance (Section 10)**: the opposite choice — a single conditional bulk
  `UPDATE ... WHERE status = 'OFFERED'` (`DeliveryAssignmentRepository.acceptIfOffered`),
  because the race here is resolved by row count (1 = won, 0 = lost), not by retrying.
- **Self-injection in `OrderService`**: the stock-decrement retry loop calls the
  `@Transactional` method through a `@Lazy`-injected self-reference so each retry goes
  through the Spring proxy and gets a genuinely fresh transaction/persistence context.

## Testing

`./gradlew test` runs the full suite (Mockito unit tests + `@SpringBootTest` integration
tests against the file-based H2 test database). The two required concurrency tests live in
`OrderPlacementIntegrationTest` (10 threads vs. `stockQuantity=5`, asserts exactly 5 succeed
and final stock is 0) and `DeliveryAssignmentIntegrationTest` (10 partners racing to accept
one assignment, asserts exactly 1 wins). Both were run repeatedly during development to rule
out flakiness, not just once.

See [`SKILLS.md`](SKILLS.md) for the repeatable, flow-specific skills followed to build and
verify this project (implementing a build-order step, adding an endpoint, writing a
concurrency test, resolving a spec gap).
