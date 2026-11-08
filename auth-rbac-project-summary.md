# Auth & RBAC Platform: Project Summary

A study project to learn **Java 21**, **Spring Boot 4 / Spring Security 7**, OAuth2, and **microservices architecture**. It builds an authentication service and a central RBAC service with global and per-application roles, plus a simple inventory app that uses them.

The project has two versions with the **same domain and rules**:

- **v1 (Modular monolith):** one application with strict module boundaries (auth, rbac, admin, inventory).
- **v2 (Microservices):** the modules become independent services behind an API gateway, with events, caching, resilience, and distributed tracing.

CI/CD, containers, and observability are set up from the first stage. Kubernetes comes at the end of v2, once there are several services to deploy.

The README should tell the migration story: what changed from v1 to v2, and why.

## Goals

- Understand OAuth2 / OpenID Connect from the provider side, not only as a consumer.
- Separate **authentication** (who you are) from **authorization** (what you may do) into distinct modules, then distinct services.
- Design a per-application role model, similar to Keycloak client roles or Entra app roles.
- Compare permissions in tokens vs. runtime authorization checks, and their trade-offs.
- Learn microservices patterns: gateway, database per service, events, resilience, distributed tracing.
- Learn to containerize, observe, and deploy the platform on Kubernetes.
- Practice modern Java 21 and contract-first API design with OpenAPI.
- Produce a clean, well-tested, documented repository for GitHub.
- Practice an AI-assisted workflow: lean agent context, documented standards, and automated AI code review in CI.

## Tech Stack

| Area | v1 | v2 adds |
|---|---|---|
| Language | Java 21 | |
| Framework | Spring Boot 4, Spring Security 7 | |
| Identity | OAuth2 Authorization Server (part of Spring Security 7) | |
| Architecture | Spring Modulith (module boundaries, in-process events) | Spring Cloud Gateway |
| Persistence | Spring Data JPA, PostgreSQL, Flyway | One database per service |
| Messaging | Modulith application events | RabbitMQ (event externalization) |
| Resilience | | Resilience4j (timeouts, circuit breaker) |
| Caching | | Caffeine (per-service permission cache) |
| Observability | Actuator, Micrometer + OpenTelemetry (metrics, logs, traces), `grafana/otel-lgtm` | Distributed traces across services |
| Build | Maven | Maven multi-module |
| API contract | OpenAPI (design-first) + OpenAPI Generator | |
| Frontend | Angular 22, OIDC client library (Auth Code + PKCE) | |
| Testing | JUnit 5, Mockito, AssertJ, Spring Security Test, Testcontainers, Modulith tests; Vitest + Playwright | Contract and end-to-end tests across services |
| Quality | Spotless, Checkstyle or Error Prone | |
| Containers | OCI images (Spring Boot buildpacks or Jib), Docker Compose for local development | One image per service |
| CI/CD | GitHub Actions, images published to GitHub Container Registry (GHCR) | Per-service pipelines |
| Kubernetes | | kind (local cluster), Kustomize, ingress controller |
| AI tooling | `AGENTS.md` + `docs/`, custom agent skills, PR-Agent (open source, by Qodo) as the AI reviewer in CI | |

---

## AI-Assisted Development

The principle is **"first make it work, then make it good."** The implementing agent focuses on making the change work and keeps a small context. Reviewing agents then make the code follow the standards. This follows Matt Pocock's approach to AI setup: `AGENTS.md` is a short map, and the details live in `docs/`, loaded only by the agents that need them.

### Repository layout for AI context

```
AGENTS.md                   # short: what the project is, how to build/test, where to find more
.pr_agent.toml              # PR-Agent configuration (review instructions, tools enabled)
docs/
├── architecture/           # how the project is organized (for humans and all agents)
│   ├── organization.md     # repo layout, build, environments
│   ├── modules.md          # modules, boundaries, public APIs, events
│   └── testing-strategy.md # test pyramid, what to test where, Testcontainers, Modulith tests
└── standards/              # coding standards (mainly for reviewing agents)
    ├── backend.md          # Java/Spring: layers, DTOs, errors, persistence, security
    └── frontend.md         # Angular: components, signals, state, HTTP, tests
.claude/skills/             # custom skills
├── code-reviewer/          # reviews a diff against docs/standards; used by PR-Agent
└── test-writer/            # (open question, see below)
```

- **`AGENTS.md`** stays short: a project summary, build and test commands, and links into `docs/`. It contains no coding standards.
- **`docs/architecture/`** describes the project's structure, its modules and the testing strategy. Any agent (or person) reads it to find its way around.
- **`docs/standards/`** holds the backend and frontend coding standards, split by layer. Reviewing agents load it; the implementing agent does not, so its context stays lean.

### Agent roles

| Agent | Context it loads | Responsibility |
|---|---|---|
| Implementer | `AGENTS.md`, `docs/architecture/` | Make the change work, with tests passing |
| Code reviewer (`code-reviewer` skill) | `docs/standards/`, `docs/architecture/` | Check that the new code follows the standards and the module boundaries |
| PR-Agent (CI) | `.pr_agent.toml` + the `code-reviewer` instructions | Run the review automatically on every pull request |
| Test writer *(open question)* | `docs/architecture/testing-strategy.md` | Write tests separately from the implementation |

**Open question: a separate test-writing agent?**
- For: tests written by an agent that didn't write the code are less likely to repeat the implementation's assumptions, and the implementer's context stays smaller.
- Against: it adds a hand-off, and a TDD-style loop (test first, then code) is harder when the two are split.
- To decide in Phase 1, once there is real code to try both approaches on.

### CI pipeline

```mermaid
flowchart LR
    B[Build + static checks<br/>Spotless, Checkstyle/Error Prone] --> T[Tests<br/>unit, Modulith, Testcontainers]
    T --> R[AI review<br/>PR-Agent]
```

- Each stage runs only if the previous one passed (`needs:` in GitHub Actions). PR-Agent never reviews code that doesn't build or pass tests, which saves tokens and keeps the review about quality, not breakage.
- **CI results as review context:** PR-Agent only sees the PR (diff, title, description, commits) and doesn't read CI results on its own. The test stage uploads its reports as a workflow artifact. The review job downloads them and passes them to PR-Agent with its `artifact_path` input (or the `[artifacts]` section of `.pr_agent.toml`). Since review only runs once tests pass, the useful inputs are coverage (JaCoCo), the test summary (counts, skipped tests) and non-blocking static-analysis warnings. Reports are truncated at about 50k characters, so pass compact summaries, not every raw Surefire XML.
- PR-Agent posts its review as PR comments. To start with, it is advisory: it does not block the merge.
- PR-Agent needs an LLM API key stored as a repository secret. Secrets are not available to pull requests from forks, so the review is skipped on those.
- **To verify in Phase 0:** PR-Agent does not load agent skills natively. Its review instructions come from `.pr_agent.toml` (e.g. `[pr_reviewer] extra_instructions`). The `code-reviewer` skill should stay the single source of truth, so we need a way to feed it in: either `.pr_agent.toml` points to the standards and summarizes them, or a CI step generates the instructions from `docs/standards/` before PR-Agent runs.

---

## RBAC Model

### Naming convention

| Concept | Format | Examples |
|---|---|---|
| Application | `<app>` | `auth`, `rbac`, `inventory` |
| Permission | `<app>:<resource>:<action>` | `inventory:item:read`, `rbac:role:assign`, `auth:user:read` |
| Application role | `<app>:<role>` | `inventory:viewer`, `inventory:editor`, `inventory:manager` |
| Global role | `<role>` | `platform-admin`, `user` |

- **Permissions** are the fine-grained actions an application checks in code.
- **Application roles** are named bundles of one application's permissions.
- **Global roles** are identity-level and cross-application, included in every token.
- **Applications register their own permissions and roles** through a manifest. The platform's own apps (`auth`, `rbac`) are registered the same way.
- **Resource-level rules** (e.g. "creator of this item") are never in the RBAC model; they stay in the application.

### Ownership of data

- The **auth** module/service owns users, credentials, and OAuth2 clients.
- The **rbac** module/service owns applications, permissions, roles, and assignments. It references users only by `sub`.
- Applications own their domain data and resource-level rules.

### Two ways an application gets permissions

| | A: Permissions in the token | B: Runtime check |
|---|---|---|
| How | Auth asks RBAC at token issuance and embeds the permissions | The app asks RBAC per request (`can user X do Y?`), with a cache |
| Speed | Fast, no extra calls | Extra call on cache miss |
| Freshness | Changes apply at token refresh | Immediate (after cache eviction) |
| Availability | Apps work even if RBAC is down | RBAC becomes a runtime dependency |

**Strategy used in this project:** A by default; B (with caching and event-driven invalidation) for sensitive operations such as deleting items or assigning roles.

### Token example

A client requests an application's scope (e.g. `inventory`); the token gets `aud` set to that application and **only that application's permissions**, plus global roles.

```json
{
  "sub": "8f2c…",
  "aud": "inventory",
  "scope": "openid inventory",
  "roles": ["user"],
  "permissions": ["inventory:item:read", "inventory:item:write"]
}
```

---

## Inventory Domain

The inventory app exists only to demonstrate how an application plugs into the platform, so the domain is intentionally minimal: **a single table, `item`**.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `name` | text | Required |
| `quantity` | integer | Required, ≥ 0 |
| `location` | text | Free text, e.g. "Warehouse A, shelf 3" |
| `created_by` | text | User `sub` from the token (never a copy of user data) |
| `created_at`, `updated_at` | timestamp | Audit fields |

**API:** list items (paginated), get item, create, update, delete.

**Manifest:**

```yaml
application: inventory
permissions:
  - inventory:item:read
  - inventory:item:write
  - inventory:item:delete
roles:
  inventory:viewer: [inventory:item:read]
  inventory:editor: [inventory:item:read, inventory:item:write]
  inventory:manager: [inventory:item:read, inventory:item:write, inventory:item:delete]
```

| Role | Can do | Demonstrates |
|---|---|---|
| `inventory:viewer` | List and view items | Basic token-based check |
| `inventory:editor` | Create and update items | Token-based checks with `@PreAuthorize` (Strategy A) |
| `inventory:manager` | Everything, including delete | Runtime check on delete (Strategy B): revoking the role blocks deletes immediately, even with an old token |

**Optional resource-level rule:** editors may update only items they created (`created_by`), while managers may update any item. This shows a rule that belongs to the application, not to the RBAC model.

---

## Observability

All telemetry is produced with **Micrometer + OpenTelemetry** and sent over OTLP to **`grafana/otel-lgtm`**: a single, free, open-source container bundling an OpenTelemetry collector, Tempo (traces), Prometheus/Mimir (metrics), Loki (logs), and Grafana (UI). It runs in Docker Compose (and in the cluster in v2's last phase).

```yaml
services:
  otel-lgtm:
    image: grafana/otel-lgtm
    ports:
      - "3000:3000"   # Grafana UI
      - "4317:4317"   # OTLP gRPC
      - "4318:4318"   # OTLP HTTP
```

- **v1:** metrics (HTTP, JVM, database pool), structured logs with trace IDs, and traces within the monolith. Custom security metrics: tokens issued, failed logins, lockouts, role changes. A Grafana dashboard for these.
- **v2:** distributed traces across services, e.g. one login followed through gateway → auth-service → rbac-service, including the token enrichment call.

---

## v1: Modular Monolith

One Spring Boot application, with modules that communicate only through public interfaces and application events. Spring Modulith verifies the boundaries in a test, so the v2 split is mechanical rather than a rewrite.

```mermaid
flowchart LR
    P[Admin portal<br/>Angular 22] -->|Auth Code + PKCE| APP
    P -->|Bearer token| APP
    subgraph APP[Monolith]
        AUTH[auth] -->|RbacApi interface| RBAC[rbac]
        ADMIN[admin API] --> AUTH
        ADMIN --> RBAC
        INV[inventory] -->|registers manifest| RBAC
        AUTH -. UserDeleted event .-> RBAC
    end
    APP --> DB[(PostgreSQL<br/>schema per module)]
    APP -->|OTLP| OBS[otel-lgtm]
```

### Modules

| Module | Responsibility |
|---|---|
| `auth` | Users, credentials, login, OAuth2 Authorization Server, token customizer |
| `rbac` | Applications, permissions, roles, assignments, manifest registration, permission queries |
| `admin` | Admin and account REST API used by the portal |
| `inventory` | CRUD on items (one table); declares its manifest |

Rules: no module touches another module's entities or tables; each module has its own database schema; cross-module calls go through a public API interface or events.

### Phase 0: Project Skeleton, CI/CD, Containers & AI Setup
- [ ] Spring Boot 4 project with Spring Modulith and empty `auth`, `rbac`, `admin`, `inventory` modules
- [ ] Modulith verification test (`ApplicationModules.of(...).verify()`) from day one
- [ ] Spotless and Checkstyle (or Error Prone)
- [ ] Container image built with Spring Boot buildpacks (`spring-boot:build-image`) or Jib
- [ ] `compose.yml` for local development: app, PostgreSQL, Mailpit, `otel-lgtm`
- [ ] Actuator health endpoints (liveness and readiness) and basic OTLP export to `otel-lgtm`
- [ ] GitHub Actions pipeline on every pull request, in stages: build + static checks → tests → AI review
- [ ] GitHub Actions: build and publish the image to GHCR on `main` and on tags
- [ ] Dependabot for Maven, npm, and GitHub Actions
- [ ] `AGENTS.md`: short project summary, build/test commands, and links into `docs/`
- [ ] `docs/architecture/`: organization, modules, testing strategy
- [ ] `docs/standards/`: backend and frontend coding standards (first version, grows with the code)
- [ ] `code-reviewer` skill that reviews a diff against `docs/standards/` and `docs/architecture/`
- [ ] PR-Agent in CI with `.pr_agent.toml`, an LLM API key secret, and review instructions sourced from the `code-reviewer` skill (see [CI pipeline](#ci-pipeline))

### Phase 1: Auth Module Foundation
- [ ] Users in PostgreSQL with a JPA-backed `UserDetailsService`; BCrypt or Argon2
- [ ] Registered clients and authorizations persisted in the database
- [ ] Authorization Code + PKCE with a login page; Client Credentials; refresh token rotation
- [ ] Persistent signing key at the JWKS endpoint; OIDC discovery working
- [ ] Flyway migrations, one schema per module

### Phase 2: RBAC Module
- [ ] Entities: Application, Permission, ApplicationRole, GlobalRole, assignments (by user `sub`)
- [ ] Naming convention enforced; applications can only define permissions in their own namespace
- [ ] Manifest registration API (idempotent upsert; removed permissions deprecated, not deleted)
- [ ] `auth` and `rbac` registered as applications with their own manifests
- [ ] Public `RbacApi` interface: permissions of a user for an application, permission check
- [ ] Seed global roles (`platform-admin`, `user`) and a default admin
- [ ] `UserDeleted` event from `auth` removes the user's assignments in `rbac`

### Phase 3: Tokens (Strategy A)
- [ ] Application scopes (e.g. `inventory`) select the target application
- [ ] Token customizer calls `RbacApi`, sets `aud`, adds that application's `permissions` and the global `roles`
- [ ] README section documenting the **token contract** (issuer, audience, claims, naming)

### Phase 4: Inventory Module
- [ ] `item` table and entity as described in [Inventory Domain](#inventory-domain), with design-first OpenAPI
- [ ] Manifest registered with `rbac` at startup
- [ ] `@PreAuthorize("hasAuthority('inventory:item:write')")` on endpoints (Strategy A)
- [ ] Delete endpoint using a runtime check through `RbacApi` (Strategy B, in-process)
- [ ] Optional: editors update only their own items (`created_by`)
- [ ] Tests for viewer, editor, and manager (allowed and forbidden actions)

### Phase 5: Admin & Account API, Hardening
- [ ] Design-first `openapi.yaml`; server interfaces generated
- [ ] Admin endpoints: users, applications, roles, assignments (protected by `auth:*` and `rbac:*` permissions)
- [ ] Account endpoints: profile, change password, my roles, authorized apps with revoke
- [ ] Self-registration, email verification, password reset (Mailpit in Docker)
- [ ] Rate limiting / account lockout; `ProblemDetail` errors; audit log of security events

### Phase 6: Observability
- [ ] Metrics, logs (with trace IDs), and traces exported to `otel-lgtm`
- [ ] Custom security metrics: tokens issued, failed logins, lockouts, role changes
- [ ] Grafana dashboard for application health and security metrics, versioned in the repo

### Phase 7: Admin Portal (Angular 22)
- [ ] Login via Authorization Code + PKCE; token interceptor; silent refresh
- [ ] Users: list, search, assign global and application roles
- [ ] Applications: registered apps, their permissions, application roles
- [ ] Inventory screen to demo the three inventory roles
- [ ] Account page for regular users
- [ ] UI driven by permissions, while the API still enforces every rule
- [ ] TypeScript client generated from OpenAPI; Playwright end-to-end tests
- [ ] Portal container image (static build served by Nginx) built and published in CI

### Phase 8: v1 Release
- [ ] Testcontainers integration tests for all OAuth2 flows and RBAC rules
- [ ] `docker compose up` runs the full platform (app, portal, database, Mailpit, `otel-lgtm`)
- [ ] README with architecture, module diagram, RBAC model, token contract, and how to run with Docker Compose
- [ ] Tag `v1.0`

---

## v2: Microservices

The modules become independent services, each with its own database. The frontend talks only to the gateway.

```mermaid
flowchart LR
    P[Admin portal] --> GW[API Gateway]
    P -->|Auth Code + PKCE| AUTH[auth-service]
    GW --> AUTH
    GW --> RBAC[rbac-service]
    GW --> INV[inventory-service]
    AUTH -->|token enrichment<br/>Client Credentials| RBAC
    INV -->|register manifest<br/>runtime checks| RBAC
    RBAC -->|RolesChanged| MQ[[RabbitMQ]]
    AUTH -->|UserDeleted| MQ
    MQ --> INV
    MQ --> RBAC
    AUTH --> ADB[(auth DB)]
    RBAC --> RDB[(rbac DB)]
    INV --> IDB[(inventory DB)]
```

All services export telemetry to `otel-lgtm` (omitted from the diagram).

### Repository Layout

```
auth-platform/
├── pom.xml                # parent POM
├── contracts/             # openapi.yaml files per service
├── auth-service/
├── rbac-service/
├── inventory-service/
├── gateway/
├── admin-portal/          # Angular 22
├── k8s/                   # Kustomize base and overlays
└── compose.yml            # services, Postgres instances, RabbitMQ, Mailpit, otel-lgtm
```

### Services

| Service | v1 module(s) | Notes |
|---|---|---|
| `auth-service` | `auth` + account parts of `admin` | Authorization server; calls rbac-service for token enrichment |
| `rbac-service` | `rbac` + RBAC parts of `admin` | Resource server; manifest registration, permission queries and checks |
| `inventory-service` | `inventory` | Resource server; registers its manifest at startup |
| `gateway` | - | Single entry point; validates tokens at the edge and relays them |

Services still validate tokens themselves (no blind trust in the gateway).

### Phase 9: Split & Infrastructure
- [ ] Multi-module Maven project; each v1 module becomes a service
- [ ] Database per service (data migrated from v1 schemas)
- [ ] Spring Cloud Gateway with token relay; routes per service
- [ ] `compose.yml` running all services with Docker DNS (no service discovery server)
- [ ] One image per service; CI builds and publishes only the services that changed
- [ ] Each service registered as an OAuth2 client where it calls others

### Phase 10: Auth ↔ RBAC over HTTP
- [ ] `RbacApi` becomes an HTTP client (`@HttpExchange`, generated from the rbac OpenAPI spec)
- [ ] auth-service authenticates to rbac-service with Client Credentials (`rbac:permissions:read` scope)
- [ ] Token enrichment over HTTP, with a timeout and a defined failure behavior (e.g. fail token issuance rather than issue a token without permissions)
- [ ] Issuer and audience validation working across the Docker network

### Phase 11: Inventory Service & Registration
- [ ] Manifest registration at startup over HTTP (Client Credentials, `rbac:register` scope)
- [ ] Registration retried with backoff if rbac-service is not ready yet
- [ ] Strategy A checks from token claims, as in v1

### Phase 12: Runtime Checks, Caching & Events
- [ ] Strategy B: runtime permission checks from inventory-service to rbac-service
- [ ] Caffeine cache of permission decisions with a short TTL
- [ ] `RolesChanged` and `UserDeleted` events externalized to RabbitMQ (Spring Modulith event externalization)
- [ ] inventory-service evicts cached permissions on `RolesChanged`; rbac-service removes assignments on `UserDeleted`
- [ ] Idempotent event handlers (safe to process the same event twice)

### Phase 13: Resilience & Distributed Tracing
- [ ] Resilience4j on all inter-service calls: timeouts, retries, circuit breaker
- [ ] Defined fallbacks: deny by default for runtime checks when rbac-service is unavailable
- [ ] Trace context propagated across HTTP calls and RabbitMQ messages
- [ ] One login traceable in Grafana across gateway, auth-service, and rbac-service
- [ ] Dashboard additions: inter-service latency, circuit breaker state, cache hit rate

### Phase 14: Kubernetes
- [ ] Local cluster with kind; ingress controller installed
- [ ] Kustomize base + `local` overlay per service: Deployments, Services, ConfigMaps, Secrets
- [ ] Gateway and auth-service exposed through the ingress; internal services not exposed
- [ ] One PostgreSQL per service (StatefulSets), RabbitMQ, Mailpit, and `otel-lgtm` in the cluster
- [ ] Liveness and readiness probes from Actuator; resource requests and limits
- [ ] Stable public hostname for the issuer (e.g. `auth.localtest.me`) so tokens are valid inside and outside the cluster
- [ ] Readiness ordering handled by registration retries, not startup dependencies
- [ ] CI job: create a kind cluster, deploy, and run end-to-end smoke tests
- [ ] Docker Compose remains the fast loop for daily development

### Phase 15: v2 Release
- [ ] Integration tests per service (Testcontainers) and end-to-end tests through the gateway
- [ ] Contract tests between services based on the OpenAPI specs
- [ ] Admin portal pointed at the gateway, unchanged otherwise
- [ ] README: how to run with Docker Compose and on Kubernetes
- [ ] README migration write-up: what changed from v1 to v2, trade-offs, and lessons learned
- [ ] Tag `v2.0`

---

## v3 (Future): Agent Access via MCP

- [ ] MCP server for the inventory service, acting as an OAuth2 resource server
- [ ] Protected Resource Metadata (RFC 9728) for authorization server discovery
- [ ] Resource indicators (RFC 8707) so tokens are bound to a single server
- [ ] Client registration for agents (Dynamic Client Registration or Client ID Metadata Documents)
- [ ] Token Exchange (RFC 8693) for downstream calls, with an `act` claim identifying the agent
- [ ] Narrow scopes, short-lived tokens, consent page naming the agent, and revocation of authorized agents

## Stretch Goals

- Social login (GitHub / Google) federated through the auth service
- Two-factor authentication (TOTP)
- Helm charts or GitOps deployment (e.g. Argo CD) on top of the Kustomize manifests

---

## Java + Spring Notes

- Use **records** for DTOs; keep JPA entities as regular classes (careful `equals`/`hashCode` with lazy loading).
- Use **sealed interfaces** with pattern-matching `switch` for result/error types.
- Use constructor injection, and keep entity ↔ DTO mapping in small mapper classes or MapStruct.
- Spring Boot 4 uses Jackson 3 and modularized starters; check the migration guide when following tutorials written for Boot 3.
- Never edit generated OpenAPI code; change the spec or generator config instead, and keep generated sources out of Git.
- Finish v1 completely (tests, CI, README, tag) before starting v2.

---

## Database, Environments & Data Integrity Notes

### Principle: the database is the source of truth

- Every invariant lives in the database: `NOT NULL`, `CHECK`, `UNIQUE`, foreign keys, defaults.
- API validation (Bean Validation on request records) exists only to give friendlier, field-level error messages. If it drifts from the database rules, the database still has the final word.
- Flyway owns the schema in every environment. Hibernate never changes it.

### Environments and configuration

| Profile | Purpose | Database |
|---|---|---|
| `dev` | Local development | PostgreSQL from `compose.yml`, wired automatically by Spring Boot's Docker Compose support |
| `prod` | Prod-like runs: `compose.prod.yml` with the published image, and Kubernetes in v2 | Configured only through environment variables |
| tests | Integration tests | Testcontainers with `@ServiceConnection`, same PostgreSQL major version as prod |

```yaml
# application.yml: shared, no secrets, no environment-specific values
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  modulith:
    runtime:
      flyway-enabled: true

---
# application-dev.yml: local defaults that just work with compose.yml
spring:
  docker:
    compose:
      enabled: true

---
# application-prod.yml: no defaults: missing variables fail at startup
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: ${DB_POOL_SIZE:10}
  docker:
    compose:
      enabled: false
```

- Dev works with zero setup; prod has no defaults for anything that matters (a missing `DB_URL` must crash at startup, never fall back to localhost).
- Secrets never go in the repo: environment variables, a gitignored `.env` for local prod-like runs, Kubernetes Secrets in v2.

### Flyway rules

- The same migrations run in every environment. Environments differ in configuration, never in schema.
- Never edit a migration once applied (Flyway checksums every file); fixes go into a new migration.
- Keep `clean` disabled (the default).
- Use Spring Modulith's module-specific Flyway migrations (`spring.modulith.runtime.flyway-enabled`, requires `spring-modulith-runtime`): each module owns its migrations, run in module dependency order. In v2, each service takes its folder with it.
- Map each entity to its module's schema explicitly, e.g. `@Table(schema = "rbac", name = "application_role")`, otherwise validation looks in `public` and reports missing tables.

### Seed data

- **Reference data needed everywhere** (global roles, the `auth` and `rbac` applications): migrations or startup manifest registration.
- **Initial admin user:** an idempotent bootstrap runner reading `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD`; it creates the admin only if none exists. Not a migration: migrations can't hash passwords properly, and credentials would end up in Git.
- **Dev-only test data** (sample users, items, test OAuth clients): a runner active only in the `dev` profile, using the real services so passwords are hashed and business rules apply.

### When migrations run

- **v1:** at application startup (Spring Boot default). Flyway takes a database lock, so concurrent instances don't migrate twice.
- **v2 (Kubernetes):** a separate step before the rollout (a Job or init container). Run the app's own image with a `migrate` profile that runs Flyway and exits, so the migrations and Modulith ordering are identical.
- **Least privilege:** the migration step uses a database user that can change the schema; the running app uses a user that can only read and write data.
- **Expand and contract** for rolling deployments: add the new structure and deploy code that handles both; remove the old structure in a later release.

### What `ddl-auto=validate` checks (and doesn't)

| Checked at startup | Not checked |
|---|---|
| Each entity's table exists, in the expected schema | Nullability (`@Column(nullable = false)` vs. the real column) |
| Each mapped column exists | Lengths and precision |
| Column types are compatible with the Java types (e.g. `Long` over `integer` fails) | Unique constraints, foreign keys, check constraints, indexes |
| Sequences used by ID generators exist | Defaults, triggers, views |
| | Tables and columns no entity maps (ignored, which enables expand/contract) |

- `@Column(nullable, length, unique)` only matter when Hibernate generates the schema. With Flyway, the migration is the truth.
- `validate` is a fast structural check; integration tests are the real guarantee.

### Constraints in migrations

Name every constraint explicitly; the name is how a violation is mapped back to a clear message.

```sql
CREATE TABLE inventory.item (
    id          uuid        PRIMARY KEY,
    name        text        NOT NULL,
    quantity    integer     NOT NULL,
    location    text,
    created_by  text        NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_item_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT ck_item_name_not_blank        CHECK (length(trim(name)) > 0)
);

CREATE TABLE rbac.permission (
    id              uuid    PRIMARY KEY,
    application_id  uuid    NOT NULL,
    name            text    NOT NULL,
    deprecated      boolean NOT NULL DEFAULT false,
    CONSTRAINT fk_permission_application FOREIGN KEY (application_id) REFERENCES rbac.application (id),
    CONSTRAINT uk_permission_name        UNIQUE (name),
    CONSTRAINT ck_permission_name_format CHECK (name ~ '^[a-z][a-z0-9-]*:[a-z][a-z0-9-]*:[a-z][a-z0-9-]*$')
);
```

Suggested prefixes: `pk_` primary key, `fk_` foreign key, `uk_` unique, `ck_` check, `ix_` index.

### Translating database errors into API errors

Handle `DataIntegrityViolationException` in the `@RestControllerAdvice`: read the PostgreSQL SQL state (what kind of violation) and the constraint name (which rule), and return a `ProblemDetail`.

```java
@ExceptionHandler(DataIntegrityViolationException.class)
ProblemDetail handleIntegrityViolation(DataIntegrityViolationException ex) {
    if (NestedExceptionUtils.getMostSpecificCause(ex) instanceof PSQLException psql
            && psql.getServerErrorMessage() != null) {
        String constraint = psql.getServerErrorMessage().getConstraint();
        HttpStatus status = switch (psql.getSQLState()) {
            case "23505" -> HttpStatus.CONFLICT;                      // unique_violation
            case "23503" -> HttpStatus.CONFLICT;                      // foreign_key_violation
            case "23502", "23514" -> HttpStatus.UNPROCESSABLE_ENTITY; // not_null, check
            default -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, constraintMessages.messageFor(constraint));
        problem.setProperty("constraint", constraint);
        return problem;
    }
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Data integrity violation");
}
```

- `constraintMessages` maps constraint names to messages (e.g. `uk_permission_name` → "A permission with this name already exists"). Unknown constraints get a generic message, so raw SQL errors never reach clients.

### Data integrity tips

- **Bean Validation only on request DTOs,** not on entities, to avoid a third copy of the rules.
- **Don't check before writing.** `existsByName(...)` followed by `save(...)` fails under concurrency; insert and translate the unique violation into a 409.
- **Watch flush timing.** JPA sends inserts at flush, often at commit, so violations can surface after the method returns. The controller advice covers this; use `saveAndFlush()` when the error is needed inside the method.
- **Database-generated columns** (e.g. `created_at DEFAULT now()`): either set them in Java or mark them as generated (Hibernate's `@Generated`) so their values are read back. Pick one approach and use it consistently.
- **References across modules are eventually consistent.** No foreign keys between module schemas (and in v2 they are separate databases). Integrity across modules is maintained by events, e.g. `UserDeleted`.
- **Test every named constraint:** one integration test per constraint that violates it and checks the API's status and message.
- **CI check:** apply all migrations to an empty Testcontainers database, then start the app with `ddl-auto=validate`.
- **Optional (stretch):** generate Hibernate's DDL to a file (`jakarta.persistence.schema-generation.scripts.action=create`) and diff it against the Flyway-migrated schema to catch nullability and length drift.
