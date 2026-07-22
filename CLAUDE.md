# CLAUDE.md

Instructions for Claude Code working in this repository. Read this file in full before
writing any code, and re-read it if you've been away from this repo for a while.

## Source of truth

`IMPLEMENTATION_SPEC.md` at the repo root is the design spec. It has already made the
scoping decisions (tech stack, domain model, state machine, API surface, concurrency
mechanisms, testing requirements). Read it in full before writing anything. Your job here
is implementation against that spec, not re-deriving scope — don't second-guess or redesign
sections of it without flagging why first.

## Build order

Follow the build order in the spec's Section 15, one numbered step at a time. Do not skip
ahead to a later step, and do not batch multiple steps into one pass — work and commit one
step at a time so the git history reflects the actual build order, not a reconstruction.

## Per-step workflow

For every step in the build order, do these in sequence — do not skip or reorder them:

1. Write the entities/repositories/services/controllers needed for that step.
2. Compile (`./gradlew compileJava`) and fix any compilation errors before moving on.
3. Boot the app (`./gradlew bootRun`) and manually exercise the new endpoint(s) with
   `curl` — the happy path, the error cases named in the spec for that flow, and any
   RBAC/ownership check involved. Do this **before** writing automated tests, so that a
   failing test later tells you something about the test, not about an undiscovered
   application bug.
4. Write the unit and/or integration tests the spec calls for at that step (Section 13).
5. Run the full test suite (`./gradlew test`), not just the new tests. For anything
   involving concurrency (the stock-decrement and assignment-acceptance tests), run the
   suite more than once — a concurrency test that passes once is not proof it passes
   reliably; re-run it enough times to trust it before moving on.
6. Commit, with a message that explains what was built and, if a scoping or
   implementation decision was made, why — write the message to be understood without
   re-reading the diff.

## Concurrency correctness is non-negotiable

Sections 9 and 10 of the spec (atomic order placement, delivery-partner assignment race)
are the most important part of this assignment. Never implement the "decrement stock" or
"accept assignment" paths as a read-then-write in application code — always use the
conditional-update-at-the-database-level pattern the spec specifies, and check the affected
row count. If you find yourself writing `findById(...)` followed by a Java-side status
check followed by `save(...)` for either of these two flows, stop — that reintroduces the
race the spec is asking you to close.

If empirical testing under the required concurrency test contradicts an illustrative
number in the spec (e.g. a retry-attempt count), it is fine to adjust the number — but:
- the actual safety property (no overselling, no double-assignment) must never be
  loosened to make a test pass
- record the change and the reasoning in a code comment at the point of the change, and in
  the README's "Deviations and scoping decisions" section

## Handling gaps in the spec

If you hit something the spec implies but never fully defines (e.g. an endpoint referenced
by ID with no creation endpoint ever specified), do not silently improvise a large solution
and do not leave it broken. Resolve it with the narrowest change consistent with the rest
of the spec, and write it up in the README's "Deviations and scoping decisions" section —
one entry per gap, stating what was missing and what you did about it.

## Scope discipline

Do not introduce anything the spec's Section 3 lists as out of scope, even if it would be
"easy" or "more correct" — no message brokers, no Docker, no OAuth provider, no real
payment/notification integrations, no geo-matching. If you think one of these is genuinely
needed, say so and stop rather than adding it unprompted.

## Commit hygiene

- One commit per build-order step (or smaller, if a step is large) — never one commit
  covering multiple steps.
- Never commit code that doesn't compile or has a failing test in the suite.
- Do not rewrite or squash history to "clean it up" — the incremental history is itself
  part of what's being evaluated.

## Do not

- Do not fabricate test results, benchmark numbers, or claims about what was manually
  verified — only report what was actually run.
- Do not add dependencies outside Section 2 of the spec without flagging it first.
- Do not mark a build-order step complete if its tests aren't passing.