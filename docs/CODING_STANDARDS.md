# Coding Standards

The single entry point for coding standards. Start here, then load **only** the files that match what you're changing.

This file is a router: it holds no rules of its own except the few that apply everywhere. The rules themselves live in [`standards/`](standards/).

## Rules that apply everywhere

- When a change makes a doc wrong, update the doc in the same PR.
- Code is formatted by tooling, not by hand: `./gradlew spotlessApply`, with the rules in [`style/`](../style/README.md). Don't review what the lint step already checks.
- Architecture (module boundaries, testing strategy) is in [`architecture/`](architecture/), not here. Standards say *how* to write code; architecture says *where* it goes.

## Route by file path

| You're changing | Read |
|---|---|
| `src/main/java/**` | [Backend](standards/backend.md) |
| `src/main/java/**/*Entity*`, JPA entities, repositories | [Backend: JPA entities](standards/backend.md#jpa-entities), [Persistence](standards/backend.md#persistence) |
| `src/main/resources/db/changelog/**` | [Backend: Persistence](standards/backend.md#persistence) |
| Controllers, `openapi*.yaml`, MapStruct mappers | [Backend: REST API](standards/backend.md#rest-api), plus [ADR 1](adr/0001-request-validation-in-openapi.md) and [ADR 2](adr/0002-mapstruct-for-mapping.md) |
| `src/test/java/**` | [Backend](standards/backend.md), plus [Testing strategy](architecture/testing-strategy.md) |
| `build.gradle`, `lombok.config` | [Backend: Lombok](standards/backend.md#lombok), [Formatting](standards/backend.md#formatting) |
| `admin-portal/**` (Phase 7) | [Frontend](standards/frontend.md) |

## Route by topic

| Topic | Where |
|---|---|
| Java language features, records, sealed types, virtual threads | [backend.md#java](standards/backend.md#java) |
| Lombok: allowed and banned annotations | [backend.md#lombok](standards/backend.md#lombok) |
| Dependency injection, visibility | [backend.md#dependency-injection](standards/backend.md#dependency-injection) |
| Entities, `equals`/`hashCode` | [backend.md#jpa-entities](standards/backend.md#jpa-entities) |
| Liquibase changesets, constraints, integrity errors | [backend.md#persistence](standards/backend.md#persistence) |
| REST, `ProblemDetail`, authorization annotations | [backend.md#rest-api](standards/backend.md#rest-api) |
| Request validation, bulk requests, mapping | [backend.md#rest-api](standards/backend.md#rest-api) |
| Tests (what to test, where, how) | [architecture/testing-strategy.md](architecture/testing-strategy.md) |
| Domain events and listeners | [backend.md#events](standards/backend.md#events) |
| Angular components, state, OIDC, frontend tests | [frontend.md](standards/frontend.md) |

## Maintaining this router

The standards are split so a reader loads only what applies. When a standards file gets long, split it, then update this file:

1. Move a section to its own file, e.g. `standards/backend/persistence.md`.
2. Point both tables above at the new file.
3. Leave nothing behind but the link: a rule lives in exactly one file.

Every standards file must be reachable from this router. A file that isn't linked here won't be read.
