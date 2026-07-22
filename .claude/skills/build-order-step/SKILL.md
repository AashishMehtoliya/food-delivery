---
name: build-order-step
description: Implement one numbered step from IMPLEMENTATION_SPECS.md Section 15 end to
  end, from code through a passing, committed test suite. Use this for every step in the
  build order, one step per invocation - never batch multiple steps together.
---

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
