---
name: pre-commit-verification
description: Before committing any change, run the full test suite and resolve any
  failures - never commit around a failing test. Use this every time before `git commit`,
  regardless of which skill produced the change.
---

**Trigger**: before every commit.

**Procedure**: run the full test suite (`./gradlew test`). If anything fails, diagnose
whether the failure is an implementation bug (the change broke real behavior) or a stale
test expectation (the test asserted something that's no longer correct given an
intentional change). Fix whichever one is actually wrong, then re-run the full suite until
it's green.

**Guardrail**: if the failing test is one of the two concurrency-safety tests
(`OrderPlacementIntegrationTest`'s stock-decrement test, `DeliveryAssignmentIntegrationTest`'s
assignment-race test), never modify the assertion to make it pass. Those assertions encode
the actual safety properties (no overselling, no double-assignment) - fix the
implementation, or stop and report to the user rather than loosening the check.
