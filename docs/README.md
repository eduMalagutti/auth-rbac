# Docs

| Folder | What's in it | Who reads it |
|---|---|---|
| [`architecture/`](architecture/) | How the project is organized: modules and boundaries, testing strategy | Everyone, including the implementing agent |
| [`CODING_STANDARDS.md`](CODING_STANDARDS.md) | Entry point for coding standards: routes to the right file in `standards/` | Reviewers (people, the `code-review` skill, PR-Agent) |
| [`standards/`](standards/) | The standards themselves, for the backend and frontend | Loaded through `CODING_STANDARDS.md` |
| [`adr/`](adr/) | Architecture decision records: one file per hard-to-reverse decision, with its context and consequences | Everyone; agents check them before working in an area |

The full project spec (roadmap, RBAC model, phases, data rules) is in [`PROJECT_SPEC_SUMMARY.md`](PROJECT_SPEC_SUMMARY.md). The project overview and how to run it are in the root [`README.md`](../README.md).

The AI setup (`AGENTS.md`, skills, PR-Agent) is based on Matt Pocock's setup; see [AI-Assisted Development](PROJECT_SPEC_SUMMARY.md#ai-assisted-development).

Rule of thumb: **architecture** describes *what exists and where*; **standards** describe *how code should be written*. When a doc stops matching the code, fix the doc in the same PR.
