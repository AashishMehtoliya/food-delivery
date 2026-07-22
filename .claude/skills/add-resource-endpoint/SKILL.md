---
name: add-resource-endpoint
description: Add a new REST endpoint (create/list/action) for an existing or new domain
  resource. Use this whenever a feature increment calls for a new API surface, e.g. a new
  controller method or a new controller entirely.
---

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
6. Manually verify with `curl` before writing the automated test, then run the full test
   suite before committing (see the `implement-feature-increment` skill).
