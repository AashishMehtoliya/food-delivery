---
name: implement-feature-increment
description: Implement one bounded increment of a feature end to end - from code through
  a passing, committed test suite. Use this for any planned unit of work broken into
  ordered steps (a build order, a task list, a single ticket), one increment per
  invocation - never batch multiple increments together.
---

1. Read the spec/requirement covering this increment in full before writing any code.
2. Write the entities/repositories/services/controllers needed for this increment only -
   no work belonging to a later increment, even if it looks like a natural extension.
3. Compile and fix all errors before moving on.
4. Manually exercise every new or changed behavior (e.g. with `curl` for a REST endpoint)
   - the happy path, the named error cases, and any RBAC/ownership check involved. Do this
   before writing automated tests, so a later test failure points at the test, not an
   undiscovered application bug.
5. Write the unit and/or integration tests this increment calls for.
6. Run the `pre-commit-verification` skill before committing.
7. Commit, with a message explaining what was built and, if a scoping or implementation
   decision was made, why.
