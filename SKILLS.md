# Skills used during development

The actual skill definitions live where Claude Code expects them, under
[`.claude/skills/`](.claude/skills), one directory per skill with a `SKILL.md`
(`name` + `description` frontmatter, then the steps) - not as a single doc. This file is
just an index for anyone browsing the repo root.

None of these existed as installed `/skill` commands at build time; they were followed as
internalized workflow and written up afterward, in skill format, for the submission record.

- **[`build-order-step`](.claude/skills/build-order-step/SKILL.md)** — implement one
  numbered step from `IMPLEMENTATION_SPECS.md` Section 15 end to end: write code, compile,
  manually verify with `curl`, write the required tests, run the full suite, commit.
- **[`add-resource-endpoint`](.claude/skills/add-resource-endpoint/SKILL.md)** — add a new
  REST endpoint: DTO -> service -> controller with the right `@PreAuthorize` -> security
  whitelist if public -> exception mapping.
- **[`concurrency-test`](.claude/skills/concurrency-test/SKILL.md)** — write and validate a
  test proving a concurrency-correctness guarantee under real simultaneous load, as used
  for the two required concurrency tests (Sections 9 and 10).
- **[`resolve-spec-gap`](.claude/skills/resolve-spec-gap/SKILL.md)** — handle something the
  spec implies but never fully defines, without silently improvising or leaving it broken.

These are process skills — how to build correctly and verifiably against this spec — not
domain-specific recipes like "how to place an order," since the spec itself already
specifies those flows in full (Sections 7-11).
