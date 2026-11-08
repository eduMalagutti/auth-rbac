# Testing Strategy

The single source of truth for how this project is tested. `/build-ticket` (implementation, test-first) and the reviewers (`/code-review`, PR-Agent) all read this file. Other docs link here instead of repeating its rules.

## Running the tests

| Kind | Mark the test class with | Gradle task | Docker |
|---|---|---|---|
| Unit | nothing | `./gradlew unitTest` | no |
| Module | `@ModuleTest` (test class in the module's package) | `./gradlew moduleTest` | yes |
| Integration | `@IntegrationTest` | `./gradlew integrationTest` | yes |
| All of them | | `./gradlew test` (also part of `build`) | yes |

`@ModuleTest` and `@IntegrationTest` (in the root test package) bundle the JUnit tag, the Spring test setup and the Testcontainers PostgreSQL, so a test never repeats that setup. CI runs the three tasks as separate steps, cheapest first.

## The shape

**Integration tests carry the weight.** Each feature is proven through its real entry point (HTTP API with a real token, real PostgreSQL), covering its success path and every way it can fail. Unit tests cover only what is cheaper and clearer to test in isolation.

| Kind | Tooling | What it covers | Speed |
|---|---|---|---|
| **Feature integration** (main) | `@IntegrationTest`, MockMvc or the HTTP client, Spring Security Test, Testcontainers (PostgreSQL 18) | Every feature end to end: success and all failure categories below | seconds |
| Unit | JUnit 5, AssertJ, Jakarta `Validator` | Request validation rules, custom constraints, hand-written mapping code, pure domain logic. No Spring context. | ms |
| Module | `@ModuleTest` (Spring Modulith's `@ApplicationModuleTest`), `Scenario` | Events a module publishes and consumes, when that's easier to see than through the API | seconds |
| Architecture | `ModularityTests` | Module boundaries and dependency cycles | ms |
| End to end (Phase 7) | Playwright | Portal login and role-based UI through the real stack | minutes |

## Feature integration tests: the checklist

Every endpoint or flow gets one test per row that applies:

| Category | Expect | Example |
|---|---|---|
| **Success** | 2xx, the response body, *and* the persisted state or published event | create an item → 201, item returned, row in `inventory.item` |
| **Not authenticated** | 401 | no token, expired token, token with the wrong audience |
| **Not allowed** | 403 | `inventory:viewer` tries to create an item |
| **Allowed** (per role that may) | 2xx | `inventory:editor` creates; `inventory:manager` deletes |
| **Invalid input** | 400 with a `ProblemDetail` naming the field | missing `name`: **one representative case per endpoint**, proving `@Valid` is wired and the error format is right. Every rule is covered by unit tests (below). |
| **Resource conflict** | 409 with a `ProblemDetail` naming the rule | username already exists; permission name already registered |
| **Rule violation** | 422 with a `ProblemDetail` | quantity below zero (a `CHECK` constraint) |
| **Not found** | 404 | get or delete an item that doesn't exist |
| **Resource-level rule** | 403 | an editor updates an item someone else created |

- **Both sides of security:** for every protected action, at least one role that may (2xx) and one that may not (403), plus the unauthenticated case (401).
- **Every named database constraint** has a test that violates it through the API and checks the status and message. Don't check-then-insert in the code; let the constraint fire and test the translation.
- **Assert the error body, not just the status:** `ProblemDetail` type, the field or constraint it names, and that no SQL or stack trace leaks.

## Unit tests

| What | How | Why here |
|---|---|---|
| **Request validation rules** | Parameterized tests with a Jakarta `Validator` on the **generated** request models: each rule valid and invalid at its boundary (e.g. `maxLength: 200` → 200 passes, 201 fails), asserting the violated field path | The rules live in `openapi.yaml` ([ADR 1](../adr/0001-request-validation-in-openapi.md)); these tests prove the spec says what we meant, in milliseconds |
| **Custom constraints** (e.g. `@UniqueBy` for duplicates in a bulk request) | Same `Validator` tests, plus the constraint's edge cases (empty list, nulls, case sensitivity) | They are our code, attached through the spec |
| **Hand-written mapping code** | Direct calls: a `default` method, `@AfterMapping` or a custom converter in a MapStruct mapper | Plain MapStruct mappings need no tests: the build fails on an unmapped field and the integration success tests check the values ([ADR 2](../adr/0002-mapstruct-for-mapping.md)) |
| **Pure domain logic** | Direct calls | Functions with no I/O, e.g. permission naming rules |

Rules that need the database ("username already exists") are **not** unit-tested: they are integration tests in the *Resource conflict* row above.

For bulk endpoints, the integration tests add: one item invalid (400 with its index in the path), duplicates inside the request (400), one item clashing with stored data (409), and a request over `maxItems` (400). Nothing is persisted in any failure case.

## Rules

- **Real PostgreSQL, never H2.** Tests use Testcontainers with the same major version as production (`TestcontainersConfiguration`).
- **Liquibase runs in tests.** The schema under test is the one the changesets create; `ddl-auto: validate` catches entity and table drift at startup.
- **Test behavior, not implementation.** Assert on API responses, persisted state and published events. Don't verify mock interactions; mock only what crosses the system boundary (e.g. an external HTTP service), never our own classes.
- **Docker is required** to run the test suite.

## Naming

- Test classes: `<Feature>Tests` for integration (e.g. `CreateItemTests`), `<ClassUnderTest>Tests` for unit tests.
- Test methods describe the behavior: `rejectsUsernameThatAlreadyExists()`, `viewerCannotCreateItem()`.

## Who writes the tests

The implementer, **test-first**: `/build-ticket` drives `/tdd`, one red-green slice at a time, at the highest seam possible. There is no separate test-writing agent. A bug found in review gets a failing test before its fix.
