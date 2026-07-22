# AI workflow

This project was built end-to-end with [Claude Code](https://claude.com/claude-code)
(Claude Sonnet 5), driven from `IMPLEMENTATION_SPECS.md` as the source of truth for scope
and design.

## Process

1. The spec was read in full before any code was written. It already made the scoping
   decisions explicit (tech stack, domain model, state machine, API surface, testing
   requirements), so the job was implementation, not design.
2. Work followed the build order in the spec's Section 15, one numbered step at a time,
   with a commit at the end of each step — the incremental history in this repo's `git log`
   is that build order, not a reconstruction after the fact.
3. Each step followed the same loop:
   - Write the entities/services/controllers/tests for that step.
   - Compile (`./gradlew compileJava`).
   - Boot the app (`./gradlew bootRun`) and exercise the new endpoints by hand with `curl`
     — happy path, the relevant error cases, and ownership/RBAC checks — before writing
     automated tests, so test failures would be about test-writing mistakes, not
     undiscovered application bugs.
   - Write the unit and/or integration tests the spec calls for at that step.
   - Run the full test suite, more than once for anything involving concurrency
     (`OrderPlacementIntegrationTest`'s and `DeliveryAssignmentIntegrationTest`'s
     multi-thread tests were re-run several times during development specifically to catch
     flakiness before trusting them).
   - Commit.
4. Where the spec had a genuine gap (an endpoint or entity implied by one part of the spec
   but never defined — e.g. no user-registration endpoint despite admin endpoints
   referencing existing users by ID), the gap was resolved with the narrowest change
   consistent with the rest of the spec and written up in `README.md`'s "Deviations and
   scoping decisions" section rather than silently improvised or left broken.
5. One implementation choice was revised after empirical testing contradicted the spec's
   illustrative numbers: Section 9 suggests a bounded retry of "up to 3 attempts" for
   optimistic-lock conflicts, but running the required 10-thread concurrency test showed 3
   attempts could let a thread lose a fair race to starvation (exhausting retries while
   stock was still available) without ever violating the actual safety property (no
   overselling, no negative stock). The bound was raised to 10 and the reasoning recorded
   in code comments and the README, rather than either silently keeping a number that
   under-served the test or quietly loosening the assertion.

## What to check if reviewing this repo

- `git log --oneline` shows one commit per build-order step (occasionally split further).
- Each commit message explains what was built and, where relevant, why a specific
  implementation or scoping choice was made — the message is written to be useful without
  re-reading the diff.
- The two concurrency-correctness sections of the spec (9 and 10) have both a focused unit
  test and a multi-threaded integration test backing them.
