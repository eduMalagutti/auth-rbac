# Backend Standards (Java 25, Spring Boot 4)

Reviewers reach this file through [`../CODING_STANDARDS.md`](../CODING_STANDARDS.md). It grows as patterns settle; when a rule changes, update it in the same PR.

## Java

- Target **Java 25**. Use modern language features where they make code clearer: records, sealed interfaces, pattern matching for `switch` and `instanceof`, `var` for obvious local types, text blocks.
- **Records** for DTOs, request/response bodies, events, value objects and configuration properties.
- **Sealed interfaces + pattern-matching `switch`** for results and errors with a fixed set of cases.
- Don't create platform threads by hand. Request handling already runs on virtual threads; for extra concurrency use `Executors.newVirtualThreadPerTaskExecutor()`.

## Lombok

Lombok is allowed only for boilerplate records can't replace:

| Allowed | Use for |
|---|---|
| `@RequiredArgsConstructor` | Constructor injection into `final` fields |
| `@Builder` | Building objects with many optional fields (also on records) |
| `@Getter` | Entities (no setters by default; add intent-revealing methods instead) |
| `@Slf4j` | Logging |

**Banned** (enforced by `lombok.config`, the build fails):

- `@Data`: generates setters, `equals`/`hashCode` and `toString` from all fields.
- `@EqualsAndHashCode`: entities must define `equals`/`hashCode` by hand.
- `@Value`: use a record.

Also avoid `@ToString` on entities (it can trigger lazy loading) and `@Setter` on entities.

## Dependency injection

- Constructor injection only, with `final` fields and `@RequiredArgsConstructor`. No `@Autowired` on fields.

```java
@Service
@RequiredArgsConstructor
class ManifestRegistrationService {
    private final ApplicationRepository applications;
    private final ApplicationEventPublisher events;
}
```

- Classes are package-private unless another package needs them. A module's public API is only what sits in its top-level package (see `docs/architecture/modules.md`).

## JPA entities

- Regular classes, never records.
- **`equals`/`hashCode` written by hand**, based on the id, and safe with Hibernate proxies:

```java
@Override
public final boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Item other)) return false;
    return id != null && id.equals(other.getId());
}

@Override
public final int hashCode() {
    return getClass().hashCode();
}
```

  Why: the id can be null before persist, and Hibernate may hand you a proxy subclass. A constant `hashCode` keeps the object in the same `HashSet` bucket before and after it gets an id; `getId()` (not the field) works through proxies.
- A protected no-args constructor for JPA; a constructor or static factory with the required fields for code.
- Never expose entities from controllers or module APIs; map to records.

## Persistence

- **Liquibase owns the schema.** Hibernate only validates it (`ddl-auto: validate`); never rely on it to create or change tables.
- Changesets are **formatted SQL** files in the owning module's folder: `src/main/resources/db/changelog/<module>/NNN-description.sql`, listed in that module's `changelog.yaml`. Shared tables go in `db/changelog/shared/`.
- Each changeset starts with `--changeset esmalagutti:<module>-NNN-description`; ids are unique across the whole project. Add `--rollback` when the change can be undone.
- A new module's `changelog.yaml` is included in `db.changelog-master.yaml` **after the modules it depends on**.
- Never edit an applied changeset (its checksum is stored); write a new one.
- Every invariant is a named constraint in the changeset (`pk_`, `fk_`, `uk_`, `ck_`, `ix_`).
- Map each entity to its module's schema: `@Table(schema = "<module>", name = "...")`.
- Don't check-then-insert (`existsBy...` then `save`); insert and translate the constraint violation into a 409.
- Bean Validation only on request models (generated from `openapi.yaml`), never on entities.

## REST API

- Design-first OpenAPI: change the spec, regenerate interfaces, implement them. Never edit generated code.
- **Validation** ([ADR 1](../adr/0001-request-validation-in-openapi.md)): field rules live in `openapi.yaml` (`required`, lengths, ranges, `pattern`, `minItems`/`maxItems`), not in Java. Rules the spec can't express are custom Bean Validation constraints attached with `x-field-extra-annotation`. Controllers validate with `@Valid`.
- **Bulk endpoints:** `maxItems` is mandatory; duplicates inside the request use a custom constraint, never `uniqueItems` (it silently merges duplicates into a `Set`); one transaction, all or nothing, every problem reported.
- **Mapping** ([ADR 2](../adr/0002-mapstruct-for-mapping.md)): MapStruct mappers, one per aggregate, inside the owning module. Unmapped target fields fail the build; ignore a field explicitly. Mappers copy data only: no business rules, repository calls or authorization. Values from outside the request (e.g. `createdBy` from the token) come in as parameters.
- Errors are `ProblemDetail` (RFC 9457). Raw exception or SQL messages never reach the client.
- Authorization with `@PreAuthorize("hasAuthority('<app>:<resource>:<action>')")`; resource-level rules (e.g. "creator of this item") stay in the module's service code.

## Events

- Events are records, named in the past tense (`UserDeleted`, `RolesChanged`), published by the module that owns the data.
- Listeners use `@ApplicationModuleListener` and must be idempotent.

## Formatting

- Rules live in [`style/`](../../style/README.md) and run in CI (`spotlessCheck`): Palantir Java Format, no unused, wildcard or module imports. Run `./gradlew spotlessApply` before committing; the build fails on unformatted code.
