# 1. Request validation lives in the OpenAPI spec

**Status:** accepted (2026-11-08)

## Context

The API is designed first: `openapi.yaml` is written before the code, and OpenAPI Generator generates the server interfaces and request/response models. Request validation rules (required fields, lengths, ranges, patterns, list sizes) could be written in the spec, in hand-written records, or in both. The database stays the final word on data integrity either way (named constraints, see `PROJECT_SPEC_SUMMARY.md`).

## Decision

- **Field rules are written once, in `openapi.yaml`:** `required`, `nullable`, `minLength`/`maxLength`, `minimum`/`maximum`, `pattern`, `minItems`/`maxItems`, `enum`. The generator turns them into Bean Validation annotations on the generated models, and controllers validate with `@Valid`. The Phase 7 TypeScript client is generated from the same spec, so the portal sees the same rules.
- **Rules the spec can't express** (rules across fields or across list items) are custom Bean Validation constraints in our code, attached to the generated models from the spec with the generator's `x-field-extra-annotation` / `x-class-extra-annotation` extensions. Generated code is never edited.
- **Rules that need the database** (e.g. "username already exists") are not request validation. They are enforced by named database constraints and translated to 409/422 `ProblemDetail` responses.
- `springdoc` is removed: it generates a spec from code, the opposite direction. Swagger UI, if wanted, serves the hand-written `openapi.yaml`.

### Collections and bulk requests

A bulk request (e.g. create many users at once) is validated in three layers, all reported together, all or nothing:

| Layer | Example | Where | Response |
|---|---|---|---|
| Each item's fields | `users[3].email` is not an email | Spec rules on the item schema; Bean Validation cascades into list items | 400, field path `users[3].email` |
| Across the request | `users[2]` and `users[5]` have the same username | Custom constraint, e.g. `@UniqueBy("username")`, attached with `x-field-extra-annotation` | 400, paths of the duplicates |
| Against stored data | `users[4]` username already exists | Database unique constraint (final word), optionally a single pre-check query to name every clashing item at once | 409 |

- **Don't use `uniqueItems: true` for this.** The generator maps it to a `Set`, so duplicate items are **silently merged** on deserialization instead of rejected; and it compares whole items, not a key such as `username`.
- **List size limits** (`minItems`/`maxItems`) are mandatory on every bulk endpoint, so one request can't ask for unbounded work.
- **All or nothing:** a bulk request runs in one transaction; any failure rejects the whole request and the response lists every problem found. Partial success (207 Multi-Status) is out of scope unless a feature needs it.
- A pre-check query for stored duplicates only improves the error message (every clashing item named at once). The unique constraint still decides, because another request can insert between the check and the write.

## Consequences

- One source for field rules, shared by backend validation, the generated client and the API docs.
- Validation rules are unit-tested on the generated models (see `docs/architecture/testing-strategy.md`).
- Custom constraints are our code: each one has its own unit tests.
- Error messages for spec rules come from Bean Validation defaults unless overridden in the spec.
