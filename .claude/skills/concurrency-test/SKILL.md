---
name: concurrency-test
description: Write and validate a test proving a concurrency-correctness guarantee (no
  overselling, no double-assignment, etc.) under real simultaneous load. Use this for the
  two required concurrency tests (Sections 9 and 10) and for any other path where multiple
  callers can race on the same row.
---

1. Set up fixtures with a deliberately small, contended resource (e.g. `stockQuantity=5`
   for 10 competing requests) so the race is guaranteed to matter.
2. Use a `CountDownLatch` to release all threads as close to simultaneously as possible,
   not just an `ExecutorService` submitted in a loop - a loop alone lets requests trickle
   in staggered, understating real contention.
3. Assert the safety property first (nothing oversold, nothing double-assigned, final
   count is exactly right) - this is non-negotiable and must never be loosened.
4. Assert the liveness property second (exactly N winners, the rest a specific error code).
   If this is flaky under a reasonable retry/lock bound, fix the bound (see the
   `resolve-spec-gap` skill) rather than loosening the assertion.
5. Re-run the full test suite multiple times before trusting a green result - a concurrency
   test that passes once is not proof it passes reliably.
