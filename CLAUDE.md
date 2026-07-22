# CLAUDE.md

Project-specific context for Claude Code sessions working in this repo. For the story of
how this codebase was originally built with AI, see [`AGENTS.md`](AGENTS.md). For the spec
this implements and the assumptions/deviations made, see
[`IMPLEMENTATION_SPECS.md`](IMPLEMENTATION_SPECS.md) and [`README.md`](README.md).

## Build & run

```
JAVA_HOME=<path to a JDK 17>            # required if the system default JDK isn't 17
./gradlew bootRun                       # dev profile, H2 file at ./data/fooddelivery
./gradlew test                          # test profile, H2 file at ./data/fooddelivery-test
```

Bootstrap admin (seeded via Flyway `V2__seed_admin_user.sql`):
`admin@fooddelivery.com` / `admin123`. Swagger UI is at `/swagger-ui.html`
(springdoc-openapi), whitelisted alongside the two public browse GETs and
`/auth/register` in `SecurityConfig`.

## Structure

Single package root `com.example.fooddelivery`, split by layer:

```
config      SecurityConfig, AsyncConfig (dedicated notification thread pool)
controller  one per resource area
dto         request/response records, never entities directly on the wire
entity      JPA entities
enums       Role, OrderStatus, AssignmentStatus, AvailabilityStatus, PaymentStatus
event       OrderStatusChangedEvent + the async notification listener/registry
exception   domain exceptions + GlobalExceptionHandler (@RestControllerAdvice)
repository  Spring Data JPA interfaces
security    AppUserPrincipal / AppUserDetailsService
service     business logic, @Transactional boundaries live here
```

## Things worth knowing before changing code

- **`Order.transitionTo(OrderStatus)` is the only way order status changes.** Every
  status-changing endpoint calls it rather than setting the field directly, so illegal
  transitions are rejected the same way everywhere. Don't add a code path that bypasses it.
- **`OrderService`'s optimistic-lock retry loop uses a `@Lazy`-injected self-reference**
  (`self.placeOrderInTransaction(...)`) so each retry attempt goes through the Spring proxy
  and gets a genuinely fresh `@Transactional` boundary. Calling `this.placeOrderInTransaction(...)`
  directly would silently defeat the retry (no new transaction, no fresh read).
- **The retry bound is 10, not the spec's illustrative "3"** — see `OrderService`'s comment
  and the README's "Deviations" section for why (3 attempts starved a legitimate winner
  under the required 10-thread concurrency test).
- **The stock decrement and the assignment-accept race use different concurrency
  mechanisms on purpose**: `MenuItem` stock uses JPA optimistic locking (`@Version` +
  `saveAndFlush`, retried on conflict); `DeliveryAssignment` acceptance uses a single
  conditional bulk `UPDATE` (`acceptIfOffered`, row-count-checked, no retry). Don't
  "normalize" one to match the other without checking Section 9 vs. Section 10 of the spec
  — they're intentionally different techniques.
- **Test database is file-based H2, shared across test classes in the same JVM run.**
  `BaseIntegrationTest.cleanDatabase()` wipes all tables before every test — if you add a
  new entity, add its repository to that cleanup in FK-safe order (children before
  parents). If the full suite ever produces confusing 404s/failures that don't reproduce
  when running a single test class in isolation, check for a stray `bootRun` process still
  holding the same H2 file — that caused real, non-reproducible-looking failures during
  development until the process was killed.
- **`NotificationRegistry` is in-memory and shared across the (often context-cached) test
  suite** — it's cleared in `BaseIntegrationTest.cleanDatabase()` too. There is deliberately
  no `Notification` entity/table (Section 6 doesn't define one); see README for why.

## Verifying changes

There's no configured linter/formatter (Checkstyle, Spotless, etc.) — "lint" for this repo
means a clean `./gradlew compileJava compileTestJava` (no warnings) plus a full
`./gradlew test` pass. For anything touching the two concurrency-critical paths (order
placement stock decrement, delivery assignment acceptance), re-run the test suite multiple
times before trusting a green result — both have dedicated multi-threaded tests
(`OrderPlacementIntegrationTest`, `DeliveryAssignmentIntegrationTest`) that are the actual
proof the concurrency guarantees hold, not just decoration.
