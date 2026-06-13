# Chatly Code

Local desktop coding-agent workspace for scanning an existing project, building a deterministic code graph, finding architecture problems, creating tasks and applying reviewed workspace changes.

This repository is structured as a conservative MVP vertical slice. Module boundaries follow the full product concept and can be split or deepened without changing the high-level contracts.

## Current Slice

- JavaFX desktop shell with overview, problems, tasks, chat, diff and terminal tabs.
- In-process application facade wiring project scan, graph, problems, tasks, git, runtime and agent flow.
- Project scanner with stack and build command detection.
- Language SPI and Java extractor with Spring layer role classification.
- Deterministic in-memory code graph.
- Architecture summary and Mermaid C4 draft with controller/service/repository layers.
- Rule-based problem detection with evidence and confidence.
- Task creation from detected problems with risk and suggested plan.
- Graph-aware chat queries over the code graph.
- OpenHands-style agent runtime: action/observation loop, tools (read/list/grep/glob/shell/patch/graph/git), command policy, event log, direct task flow, approve/verify/accept/rollback.
- Workspace root enforcement and text patch application.
- CLI git status, diff, checkpoint and rollback.
- Runtime command execution for project verification.
- Conversation history for user, agent and system messages.
- LLM gateway abstraction with Z.AI support and a safe local no-op fallback.
- English and Russian UI localization.

## Modules

```text
app-desktop          JavaFX UI
app-server           UI-facing facade
agent-runtime        agent plan and orchestration
project-domain       opened project scan state
code-graph           graph indexing and queries
language-spi         language plugin contracts
language-java        Java/Spring extractor plugin
architecture-engine  C4/Mermaid architecture draft
problem-detector     architecture problem rules
task-manager         engineering task planning
workspace-safety     safe workspace reads and patches
runtime-service      process command execution
git-service          git status, diff, checkpoint, rollback
conversation-service durable in-memory conversation history
llm-gateway          provider abstraction
localization         i18n resource bundles
shared-kernel        ids, time, results, events
```

## LLM Provider

Z.AI is selected by default when an API key is present. Without a key, Chatly Code stays usable through deterministic local analysis and reports `LLM offline` in the toolbar.

Environment variables:

```bash
CHATLY_LLM_PROVIDER=zai
ZAI_API_KEY=...
ZAI_MODEL=glm-4.5
ZAI_BASE_URL=https://api.z.ai/api/paas/v4
```

Equivalent JVM system properties are also supported:

```bash
-Dchatly.llm.provider=zai
-Dchatly.llm.zai.apiKey=...
-Dchatly.llm.zai.model=glm-4.5
-Dchatly.llm.zai.baseUrl=https://api.z.ai/api/paas/v4
```

## Build

Gradle is the only build tool for this project.

```bash
gradle build
```

## Product Loop

```text
Open local project
-> scan files
-> build deterministic code graph
-> show overview, problems and tasks
-> ask graph-aware questions
-> plan agent work for a task
-> approve and run verification commands
-> review diff
-> accept or rollback
```
