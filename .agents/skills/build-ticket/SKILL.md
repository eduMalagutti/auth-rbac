---
name: build-ticket
description: "Build one GitHub issue of this repo test-first, then review the diff for correctness bugs and fix them before committing. This repo's replacement for /implement: coding standards are NOT reviewed here; they are reviewed in the pipeline (PR-Agent) or on demand with /code-review."
disable-model-invocation: true
---

# Build ticket

Matt Pocock's `/implement`, adapted to this repo's rule: **first make it work, then make it good.** This skill makes it work: the code does what the ticket asks, with tests, and without bugs. Making it good (coding standards, smells, spec conformance) happens in review, mainly in the pipeline.

## 1. Fetch the ticket

The user passes an issue number. Fetch it with `gh issue view <n> --json number,title,body,comments` and state its title. If the reference is ambiguous, ask.

Check you are on the issue's branch, `<n>-<slug>`. If not, stop and ask: don't create or switch branches yourself.

## 2. Load only what implementation needs

Read `AGENTS.md`, then the docs it points to that the ticket touches, usually `docs/architecture/modules.md` and `docs/architecture/testing-strategy.md`.

Do **not** load `docs/CODING_STANDARDS.md` or `docs/standards/`. They are for review; loading them here costs context and pulls the work toward polish before it works.

## 3. Build test-first

Call the Skill tool with `tdd`, at pre-agreed seams: one red-green slice at a time, highest seam possible. Tests follow `docs/architecture/testing-strategy.md` (real PostgreSQL through Testcontainers, never H2).

While working:

- compile often: `./gradlew compileJava compileTestJava`
- run one test class often: `./gradlew test --tests '<TestClass>'`
- mark each test class by kind: nothing for unit, `@ModuleTest`, or `@IntegrationTest` (see the testing strategy)
- at the end, format and run everything once: `./gradlew spotlessApply build`

## 4. Bug review

When the build is green, review the change for **correctness bugs only**. Use a sub-agent with a fresh context window, so the review isn't biased by how the code was written. If the harness has no sub-agents, do it yourself, reading only the diff.

Give the sub-agent:

- The diff command: `git diff $(git merge-base v1-modular-monolith HEAD)` (branch commits plus uncommitted work). For v2 work, use the v2 integration branch.
- The ticket body.
- This brief: "Find correctness bugs in this diff: wrong logic, missed edge cases, null handling, transaction and concurrency mistakes, missing or wrong authorization checks, wrong token claims, Liquibase changesets that would fail or lose data, and tests that pass without asserting the behavior. Do NOT report style, naming, code smells or coding-standard issues: those are reviewed later. For each bug: file and line, a concrete failing scenario, and your confidence. Under 400 words."

## 5. Fix what's real

For each bug the review found that you can confirm: write a failing test that shows it, then fix it (red, then green). Dismiss findings you can't reproduce, and say why in the final report. Run `./gradlew spotlessApply build` again.

## 6. Commit

Commit to the current branch. The message references the issue (`#<n>`), so `/code-review` and PR-Agent can find the spec.

Report: what was built, the bugs found and fixed, findings dismissed and why, and anything in the ticket not done yet. Standards and spec review happen on the PR.
