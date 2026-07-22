# Skills, practices, and resources used during development

Distinct from `CLAUDE.md` (the rules that governed how this codebase was built), this
documents the concrete skills, testing practices, and tooling actually used while building
this project.

## Testing

- **Mockito unit tests** for service-layer business logic (`*ServiceTest`) — repositories
  and collaborators mocked, so these run fast and isolate the logic under test from the
  database and HTTP layer entirely (e.g. `OrderServiceTest`, `RestaurantOrderServiceTest`,
  `DeliveryAssignmentServiceTest`, `RatingServiceTest`).
- **`@SpringBootTest` integration tests** (`RANDOM_PORT` + `TestRestTemplate`) for real
  end-to-end HTTP flows against the actual H2 test database — full request/response cycle
  through Spring Security, validation, the service layer, and Flyway-migrated schema.
- **Concurrency testing**: `ExecutorService` + `CountDownLatch` to fire N requests as close
  to simultaneously as possible (`OrderPlacementIntegrationTest`'s 10-thread stock-decrement
  test, `DeliveryAssignmentIntegrationTest`'s 10-partner assignment-race test). These were
  re-run several times during development — a concurrency test that passes once is not proof
  it passes reliably.
- **Manual verification before automated tests**: every new endpoint was exercised by hand
  with `curl` (happy path, the error cases named in the spec, RBAC/ownership checks) before
  writing the corresponding automated test, so a later test failure would point at a test
  bug, not an undiscovered application bug.
- **Shared test fixtures**: `BaseIntegrationTest` centralizes entity creation helpers and a
  per-test database cleanup (the H2 datasource is file-based and persists across test runs,
  so each test starts from a clean slate rather than relying on rollback).

## Tooling and resources

- **Gradle wrapper** (`./gradlew`) for a reproducible build across environments — no
  globally installed Gradle version assumed.
- **H2 Console** (`/h2-console`) for inspecting live persisted data during manual
  verification of a running instance.
- **Swagger UI / OpenAPI** (`springdoc-openapi-starter-webmvc-ui`, `/swagger-ui.html`) for
  browsable, testable API documentation generated directly from the controllers.
- **Flyway** for versioned, H2-compatible schema migrations instead of `ddl-auto=update`,
  so the schema history is explicit and reviewable.
- **curl** for direct HTTP-level manual verification against a locally running instance,
  including concurrent request bursts during ad-hoc checks before formalizing a test.

## AI-assisted development

No packaged Claude Code Skills (the `/skill` slash-command mechanism) were invoked for this
build. All work was done through direct agentic tool use — reading and writing files,
running Gradle/curl/git commands, inspecting logs and test reports — rather than any
pre-packaged skill script. See `CLAUDE.md` for the rules that governed that process.
