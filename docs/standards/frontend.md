# Frontend Standards (Angular 22)

The admin portal starts in Phase 7. This file is filled in then, with the patterns actually used.

Planned scope:

- Standalone components, signals for state, the new control flow (`@if`, `@for`).
- OIDC login with Authorization Code + PKCE; token interceptor; silent refresh.
- API calls only through the TypeScript client generated from OpenAPI.
- UI driven by permissions, while the API still enforces every rule.
- Tests: Vitest for units, Playwright for end to end.
