---
name: resolve-spec-gap
description: Handle a genuine gap in the spec - something one part of the spec implies
  (an endpoint referenced by ID, a transition with no trigger) but never actually defines
  elsewhere. Use this instead of silently improvising or leaving the gap unresolved.
---

1. Confirm it's a real gap, not a missed reading - re-read the relevant spec sections in
   full first.
2. Resolve it with the narrowest change consistent with the rest of the spec's design
   (matching existing patterns: ownership checks, exception types, DTO shapes) rather than
   a novel mechanism.
3. If empirical testing (see the `concurrency-test` skill) contradicts an illustrative
   number in the spec, it's fine to change the number, but the actual safety property must
   never be loosened to make a test pass.
4. Write the gap and the resolution up as one entry in the README's "Deviations and scoping
   decisions" section: what was missing, what was done about it, and why.
