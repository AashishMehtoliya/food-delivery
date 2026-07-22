# Skills used during development

Each skill below follows the standard Claude Code skill shape (`name`, `description`, then
the steps to execute) and documents a specific, repeatable flow that was actually followed
while building this project — not a general list of tools. None of these existed as
installed `/skill` commands at build time; they were followed as internalized workflow and
are written up here, after the fact, in skill format for the submission record.

---

```
---
name: build-order-step
description: Implement one numbered step from IMPLEMENTATION_SPECS.md Section 15 end to
  end, from code through a passing, committed test suite. Use this for every step in the
  build order, one step per invocation - never batch multiple steps together.
---
```

1. Read the spec section(s) this step covers in full before writing any code.
2. Write the entities/repositories/services/controllers needed for this step only - no
   work belonging to a later step, even if it looks like a natural extension.
3. Compile (`./gradlew compileJava`) and fix all errors before moving on.
4. Boot the app (`./gradlew bootRun`) and manually exercise every new endpoint with `curl`:
   the happy path, every error case the spec names for this flow, and any RBAC/ownership
   check involved. Do this before writing automated tests, so a later test failure points
   at the test, not an undiscovered application bug.
5. Write the unit and/or integration tests this step calls for (cross-check against
   Section 13's list of required tests).
6. Run the full test suite (`./gradlew test`), not just the new tests.
7. Commit, with a message explaining what was built and, if a scoping or implementation
   decision was made, why.

---

```
---
name: add-resource-endpoint
description: Add a new REST endpoint (create/list/action) for an existing or new domain
  resource. Use this whenever a build-order step calls for a new API surface, e.g. a new
  controller method or a new controller entirely.
---
```

1. Define request/response DTOs as Java records with Bean Validation annotations - never
   accept or return JPA entities directly on the wire.
2. Put business logic (ownership checks, state checks, persistence) in a `@Service` class,
   not the controller. Ownership/authorization logic beyond the role check belongs here,
   not in `@PreAuthorize` alone.
3. Add the controller method with the correct `@PreAuthorize` for the role(s) allowed to
   call it, per the spec's RBAC table.
4. If the endpoint must be public, add it to `SecurityConfig`'s `permitAll` matchers
   explicitly - default is authenticated.
5. Map every new failure path to an existing domain exception where one fits; only add a
   new exception class if the failure is genuinely a new category, and wire it into
   `GlobalExceptionHandler` immediately.
6. Manually verify with `curl` before writing the automated test (see `build-order-step`).

---

```
---
name: concurrency-test
description: Write and validate a test proving a concurrency-correctness guarantee (no
  overselling, no double-assignment, etc.) under real simultaneous load. Use this for the
  two required concurrency tests (Sections 9 and 10) and for any other path where multiple
  callers can race on the same row.
---
```

1. Set up fixtures with a deliberately small, contended resource (e.g. `stockQuantity=5`
   for 10 competing requests) so the race is guaranteed to matter.
2. Use a `CountDownLatch` to release all threads as close to simultaneously as possible,
   not just an `ExecutorService` submitted in a loop - a loop alone lets requests trickle
   in staggered, understating real contention.
3. Assert the safety property first (nothing oversold, nothing double-assigned, final
   count is exactly right) - this is non-negotiable and must never be loosened.
4. Assert the liveness property second (exactly N winners, the rest a specific error code).
   If this is flaky under a reasonable retry/lock bound, fix the bound (see
   `resolve-spec-gap`) rather than loosening the assertion.
5. Re-run the full test suite multiple times before trusting a green result - a concurrency
   test that passes once is not proof it passes reliably.

---

```
---
name: resolve-spec-gap
description: Handle a genuine gap in the spec - something one part of the spec implies
  (an endpoint referenced by ID, a transition with no trigger) but never actually defines
  elsewhere. Use this instead of silently improvising or leaving the gap unresolved.
---
```

1. Confirm it's a real gap, not a missed reading - re-read the relevant spec sections in
   full first.
2. Resolve it with the narrowest change consistent with the rest of the spec's design
   (matching existing patterns: ownership checks, exception types, DTO shapes) rather than
   a novel mechanism.
3. If empirical testing (see `concurrency-test`) contradicts an illustrative number in the
   spec, it's fine to change the number, but the actual safety property must never be
   loosened to make a test pass.
4. Write the gap and the resolution up as one entry in the README's "Deviations and scoping
   decisions" section: what was missing, what was done about it, and why.

---

## Note on scope

These are process skills - how to build correctly and verifiably against this spec - not
domain-specific "recipes" like "how to place an order," since the spec itself already
specifies those flows in full (Sections 7-11). Applying `build-order-step` and
`add-resource-endpoint` consistently is what produced that domain behavior.
