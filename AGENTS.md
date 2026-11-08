# AGENTS.md

Auth & RBAC platform: an OAuth2 / OpenID Connect provider (`auth`) and a central role-based access control service (`rbac`), plus a demo `inventory` app that uses them. A study project, built first as a modular monolith (v1, current) and later split into microservices (v2).

**Stack:** Java 25, Spring Boot 4, Spring Security 7, Spring Modulith, PostgreSQL, Liquibase, Gradle, Testcontainers.

## Commands

```bash
./gradlew build          # compile, check formatting, run all tests (needs Docker)
./gradlew spotlessApply  # format code (rules in style/); run before committing
./gradlew unitTest       # or moduleTest / integrationTest: one kind of test (see docs/architecture/testing-strategy.md)
./gradlew bootRun        # run the app; starts PostgreSQL, Mailpit and Grafana from compose.yaml
```

## Where things are

- `src/main/java/com/esmalagutti/authrbac/<module>/`: one package per module (`auth`, `rbac`, `admin`, `inventory`).
- `src/main/resources/db/changelog/`: Liquibase. One folder per module, plus `shared/`; all included from `db.changelog-master.yaml`.
- `docs/`: everything else. Start at [`docs/README.md`](docs/README.md).

## Rules that break the build or the design

- **Module boundaries:** a module uses another module only through the types in its top-level package or its events. Never import from another module's subpackages, and never touch another module's tables. `ModularityTests` enforces this. Details: [`docs/architecture/modules.md`](docs/architecture/modules.md).
- **Liquibase owns the schema.** Hibernate only validates it. Never edit an applied changeset; add a new one in the owning module's folder.
- **Lombok:** `@Data`, `@EqualsAndHashCode` and `@Value` fail the build. Use records for data; write `equals`/`hashCode` by hand on entities.
- **Tests use real PostgreSQL** through Testcontainers, never H2. See [`docs/architecture/testing-strategy.md`](docs/architecture/testing-strategy.md).

## How we work

The AI workflow follows Matt Pocock's setup and skills (`.agents/skills/`; run `/ask-matt` to pick one), except that tickets are built with **`/build-ticket`, not `/implement`**: it ends with a bug review, and the standards review runs on the PR. How it applies here: [`docs/PROJECT_SPEC_SUMMARY.md#ai-assisted-development`](docs/PROJECT_SPEC_SUMMARY.md#ai-assisted-development).

- **First make it work, then make it good.** When implementing, focus on working code with passing tests, written test-first (`/tdd`). Coding standards are applied in review, from [`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md); load them when you are reviewing, not when implementing.
- **One phase = one issue = one branch = one PR.** Branch `<issue-number>-<slug>` (e.g. `2-auth`). v1 PRs target `v1-modular-monolith`, not `main`.
- Commit messages reference the issue (`#2`). The `code-review` skill uses it to find the spec to review against.
- When a change makes a doc wrong, fix the doc in the same PR.
- Writing style for docs, issues and commits: plain and direct; no em dashes or en dashes (use a colon, comma or parentheses).

The full plan (roadmap, RBAC model, phases) is in [`docs/PROJECT_SPEC_SUMMARY.md`](docs/PROJECT_SPEC_SUMMARY.md). Read it only when you need the bigger picture; it is long.
