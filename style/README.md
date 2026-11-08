# Style

Code style is enforced by tooling, not by review. The rules live here and run in CI.

| File | What it does |
|---|---|
| [`spotless.gradle`](spotless.gradle) | Spotless rules, applied from `build.gradle`: Palantir Java Format, no unused, wildcard or module imports, formatting for `.gradle` files, and whitespace hygiene for Markdown, YAML, SQL and TOML |
| [`../.editorconfig`](../.editorconfig) | The same basics for editors (indentation, final newline), so code is close to formatted before Spotless runs |

```bash
./gradlew spotlessApply   # fix everything
./gradlew spotlessCheck   # what CI runs; fails on any difference
```

Rules that need judgement, not a formatter, are coding standards: see [`docs/CODING_STANDARDS.md`](../docs/CODING_STANDARDS.md). Prefer adding a rule here (or another automated check) over writing a standard when the rule is mechanical.
