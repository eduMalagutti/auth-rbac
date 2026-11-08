# 2. MapStruct maps between API models and entities

**Status:** accepted (2026-11-08)

## Context

Requests and responses are generated from `openapi.yaml` (see ADR 1) and have many fields; entities are hand-written JPA classes. Mapping between them by hand is repetitive, and nothing catches a field that was forgotten.

## Decision

- Mapping uses **MapStruct**: one `@Mapper` interface per aggregate, inside the owning module, package-private like the rest of the module's internals.
- **Global defaults** (compiler arguments in `build.gradle`):
  - `unmappedTargetPolicy=ERROR`: a target field nobody maps fails the build. Ignoring a field is an explicit `@Mapping(target = "...", ignore = true)`.
  - `defaultComponentModel=spring`: mappers are Spring beans, injected with `@RequiredArgsConstructor`.
- `lombok-mapstruct-binding` lets MapStruct use Lombok-generated getters, constructors and builders.
- **Mappers only copy and convert data.** No business rules, no repository calls, no authorization. Values that come from outside the request (e.g. `createdBy` from the token) are passed in as mapper parameters.
- Mapping never crosses modules: a module maps its own entities to its own API models.

## Consequences

- Plain mappings need no unit tests: the build checks completeness, and each feature's integration success test checks the values end to end.
- Hand-written mapping code (a `default` method, `@AfterMapping`, a custom `qualifiedByName` converter) is our logic and gets unit tests.
- One more annotation processor in the build; generated mappers appear under `build/generated/`.
