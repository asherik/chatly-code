# Chatly Code

Local desktop coding-agent workspace for scanning an existing project, building a deterministic code graph, finding architecture problems, creating tasks and applying reviewed workspace changes.

This repository is structured as a conservative MVP vertical slice. Module boundaries follow the full product concept and can be split or deepened without changing the high-level contracts.

## Current Slice

- JavaFX desktop shell.
- In-process application facade.
- Project scanner with stack and build command detection.
- Language SPI and Java extractor.
- Deterministic in-memory code graph.
- Architecture summary and Mermaid C4 draft.
- Rule-based problem detection.
- Task creation from detected problems.
- Workspace root enforcement and text patch application.
- Runtime, git and conversation service contracts.
- LLM gateway abstraction with a no-op local implementation.

## Build

Gradle is the only build tool for this project.

```bash
gradle build
```
