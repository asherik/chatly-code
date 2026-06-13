# Implementation Blueprint

## Purpose

This file turns the product concept into an implementation-ready blueprint. It exists so an AI coding agent can build the desktop product in controlled slices instead of inventing architecture during coding.

The implementation must be boring where infrastructure is boring and strong where the product creates value.

Core rule:

```text
Use Spring/JVM ecosystem first. Write custom code only for product-specific value.
```

## Non-Negotiable Stack

- Java 25 LTS.
- Spring Boot 4.
- Spring Framework 7.
- Spring AI 2.x or latest stable compatible version.
- JavaFX 26.
- Gradle as the mandatory build system.
- GraalVM JDK as build/runtime option.
- SQLite.
- Liquibase XML migrations.
- Spring Data JPA + Hibernate for normal domain persistence.
- Spring JDBC/JdbcClient for high-volume code graph tables.
- Spring AI Agent Utils for coding-agent tools, skills and subagent orchestration.
- Lombok.
- MapStruct.
- JSpecify.
- Micrometer.
- SLF4J/Logback.
- Docker/Podman integration behind an interface.
- MCP integration behind an interface.
- jpackage + install4j for distribution.

Do not introduce Kotlin.
Do not introduce DuckDB.
Do not introduce Flyway.
Do not introduce Maven.

The Maven restriction applies to Chatly Code's own build. User projects may use Maven, Cargo, npm, pnpm, yarn, pytest, Go, Makefile or any other build/test workflow. The product must detect and run the target project's own commands through the command policy.

## Coding Agent Foundation

Do not build the coding-agent tool stack from scratch.

Use the library from:

```text
C:\projects\chatly-code\examples\spring-ai-agent-utils-main
```

as the first-choice implementation base for:

- file read/write/edit tools;
- shell command execution tools;
- grep/search and glob tools;
- todo/task tracking;
- skills;
- subagents;
- memory;
- A2A remote subagent integration.

Chatly Code should integrate these tools through adapters:

```text
Spring AI Agent Utils tool
-> Chatly policy wrapper
-> WorkspaceSafety / RuntimeService / EventStore
-> action/observation event
-> JavaFX UI
```

Do not call library tools directly from UI controllers.
Do not bypass Chatly Code approval, sandbox, logging, diff or rollback policy.

## Repository Layout

Recommended top-level modules:

```text
chatly-code/
  app-desktop/              JavaFX application shell
  app-server/               Spring Boot UI-facing application backend
  agent-server/             agent execution boundary
  app-backend/              Spring Boot local backend
  domain/                   domain model and use cases
  persistence/              JPA, JDBC, SQLite, Liquibase
  code-graph/               AST extraction, graph indexing, resolver, context builder
  language-spi/             stable language plugin contracts
  language-java/            Java/Spring analyzer plugin
  language-rust/            Rust analyzer plugin
  language-typescript/      TypeScript/JavaScript analyzer plugin
  agent-runtime/            agent loop, tools, plan, execution, events
  sandbox-service/          process/docker/remote runtime lifecycle
  runtime-service/          command execution, terminal streams, command observations
  conversation-service/     durable conversation and trajectory history
  git-service/              status, changed files, diff, checkpoint, commit, rollback
  llm-gateway/              Spring AI based provider abstraction
  workspace-safety/         git checkpoints, patching, rollback, command policy
  c4-generator/             C4 model and Mermaid generation from graph evidence
  mcp-integration/          MCP client/server integration
  docker-integration/       Docker/Podman adapters
  shared-kernel/            shared value objects, errors, ids, time, events
  test-fixtures/            sample projects and test utilities
```

If the first prototype is a single Gradle build with fewer modules, keep the same package boundaries from day one.

## Package Rules

Use package names that express ownership:

```text
com.chatlycode.desktop
com.chatlycode.project
com.chatlycode.task
com.chatlycode.agent
com.chatlycode.agent.tool
com.chatlycode.llm
com.chatlycode.graph
com.chatlycode.graph.extract
com.chatlycode.graph.resolve
com.chatlycode.graph.context
com.chatlycode.workspace
com.chatlycode.c4
com.chatlycode.persistence
com.chatlycode.i18n
com.chatlycode.update
```

Avoid generic root packages like `service`, `manager`, `utils`, `common` unless the class has no better domain owner.

## Domain And Plugin Boundaries

Implementation must follow `20_domain_boundaries_and_language_plugins.md`.

Rules:

- use package-by-domain;
- keep domain/application/adapters separate;
- do not create god services;
- do not create god files;
- keep UI controllers thin;
- keep language-specific logic out of generic core;
- Java/Spring, Rust and TypeScript logic must live in language plugins;
- plugins depend on stable SPI and normalized graph contracts;
- core does not depend on plugin internals;
- every language plugin should be extractable as a separate library later.

Bad:

```text
GraphQueryService imports SpringFrameworkResolver
ProblemDetector imports JavaControllerRule directly
AgentPromptAssembler scans Java annotations manually
```

Good:

```text
LanguagePluginRegistry loads JavaLanguagePlugin
ProblemDetector runs registered ProblemRule instances
PromptAssembler consumes normalized graph/problem/task evidence
```

## First Vertical Slice

Build the first usable slice before adding broad features.

The first slice:

```text
Open local project
-> scan files
-> build deterministic code graph
-> show project overview
-> create one detected task
-> ask agent for plan
-> apply one safe patch
-> show diff
-> run detected target-project test/build command
-> accept or rollback
```

This slice proves the whole product loop.

Do not start with a large settings system, marketplace, cloud sync, plugin store or complex multi-agent UI before this works.

OpenHands parity must be present in this slice:

- user can type a direct task;
- agent can read files;
- agent can edit files through controlled patches;
- agent can run approved commands in process runtime;
- action/observation history is visible;
- terminal output is visible;
- git changes and diff are visible;
- user can accept or rollback.

## Desktop App Bootstrap

JavaFX starts the desktop shell. Spring Boot owns application services.

Rules:

- JavaFX controllers are thin.
- JavaFX controllers call ViewModels or application services.
- Spring creates services, repositories, mappers, graph engine, agent runtime and integrations.
- Long work never runs on the JavaFX Application Thread.
- UI updates return to the JavaFX Application Thread.
- All visible strings come from i18n resource bundles.

Recommended bootstrap classes:

```text
ChatlyCodeApplication
DesktopLauncher
SpringJavaFxBridge
MainWindowController
MainWindowViewModel
```

## Persistence Blueprint

Use one local SQLite database file per application profile, with clear schema ownership.

Product state uses JPA/Hibernate:

- projects;
- settings;
- model profiles;
- tasks;
- conversations;
- agent runs;
- tool events;
- update state;
- user preferences.

Code graph uses Spring JDBC/JdbcClient:

- `code_files`;
- `code_nodes`;
- `code_edges`;
- `code_unresolved_refs`;
- `code_symbols`;
- `code_scan_runs`;
- `code_graph_metadata`.

Liquibase XML changelog layout:

```text
src/main/resources/db/changelog/db.changelog-master.xml
src/main/resources/db/changelog/product/001-create-projects.xml
src/main/resources/db/changelog/product/002-create-tasks.xml
src/main/resources/db/changelog/graph/001-create-code-files.xml
src/main/resources/db/changelog/graph/002-create-code-nodes.xml
src/main/resources/db/changelog/graph/003-create-code-edges.xml
```

Rules:

- every schema change is a Liquibase XML changeset;
- changeset ids are stable;
- destructive changes include rollback notes;
- graph rebuild must not corrupt product state;
- large graph writes use explicit transactions and batches.

## Code Graph Blueprint

Implement the Java version of the CodeGraph-style pipeline described in `13_code_graph_engine.md`.

Primary flow:

```text
ProjectScanner
-> FileInventoryService
-> LanguageDetector
-> TreeSitterAstExtractor
-> LanguageExtractor
-> CodeGraphWriter
-> ReferenceResolver
-> FrameworkResolver
-> GraphQueryService
-> AgentContextBuilder
-> PromptAssembler
```

The graph must be deterministic and must not depend on the LLM.

Rules:

- LLM can explain graph facts, not invent them.
- unresolved references are stored and visible.
- edge provenance is stored.
- confidence is explicit.
- generated files are detected and skipped unless configured.
- context returned to the agent is bounded.

## Agent Runtime Blueprint

Agent runtime is a state machine, not an uncontrolled chat loop.

Core states:

```text
Created
-> UnderstandingProject
-> Planning
-> WaitingForApproval
-> Executing
-> Testing
-> WaitingForReview
-> Completed
```

Failure states:

```text
Blocked
Failed
Cancelled
RolledBack
```

Rules:

- no file edit without checkpoint;
- no hidden shell commands;
- every tool call is logged;
- repeated failed actions are capped;
- agent must explain plan before edits;
- all edits go through `workspace-safety`;
- user can cancel long operations.

## Prompt Assembly Blueprint

Prompt assembly must be evidence-based.

Required input:

- user task;
- current project summary;
- relevant graph nodes and edges;
- relevant files;
- detected framework conventions;
- previous failed attempts;
- current git state;
- allowed tools;
- command policy;
- output format contract.

Prompt assembler must not dump the whole repository into the model.

The target format:

```text
Task
Project Facts
Relevant Architecture
Relevant Files
Constraints
Allowed Tools
Expected Plan Format
Expected Patch/Test Summary Format
```

## C4 Blueprint

C4 generation uses graph evidence first and LLM wording second.

Flow:

```text
GraphQueryService
-> ArchitectureInferenceService
-> C4ModelBuilder
-> MermaidC4Renderer
-> DiagramReviewPanel
```

Rules:

- generated diagrams link back to evidence where possible;
- uncertain relationships are marked as inferred;
- user can edit names/descriptions;
- diagrams can be exported as Markdown/Mermaid.

## Multi-Agent Blueprint

Multi-agent work is coordinated, not chaotic.

Recommended roles:

- Coordinator agent: owns task state and final decisions.
- Architect agent: reads graph and proposes design.
- Implementer agent: prepares patch.
- Reviewer agent: checks diff, risks and missing tests.
- Test agent: runs build/test commands and summarizes failures.

Concurrency rules:

- multiple agents may read in parallel;
- only one writer may edit a file at a time;
- WorkspaceSafety serializes patches;
- Coordinator resolves conflicts;
- all subagent outputs become durable events.

## UI Blueprint

First production-quality screens:

- Project picker.
- Project dashboard.
- Architecture map.
- Problems and tasks board.
- Agent run screen.
- Diff review screen.
- Settings screen.
- Update prompt dialog.

The main screen should show:

- project health;
- scan status;
- current architecture summary;
- problem/task list;
- agent activity;
- last diff/test result.

Do not build a marketing landing page inside the app.

## Quality Gates For AI-Generated Code

Every implementation task must satisfy:

- compiles;
- tests added or explicitly justified;
- no JavaFX Application Thread blocking;
- no Entity leakage to UI;
- no direct repository calls from controllers;
- no raw LLM HTTP calls outside `llm-gateway`;
- no schema changes without Liquibase XML;
- no custom framework replacement without documented reason;
- logs do not expose secrets;
- rollback path exists for file edits.

## MVP Acceptance Criteria

MVP is acceptable only when:

- app opens a real local project;
- initial scan completes without freezing UI;
- code graph is persisted in SQLite;
- graph can answer symbol/file relationship queries;
- project overview is generated from graph evidence;
- tasks are separated into bug/problem/improvement/refactor;
- agent can produce a plan before editing;
- user can approve or reject;
- patch is applied through controlled API;
- diff is visible;
- build/test command can run;
- rollback works;
- English and Russian UI resources exist;
- installer can be produced for at least one target OS.

## What An AI Implementer Must Not Do

- Do not skip the graph and rely only on grep plus LLM.
- Do not put business logic into JavaFX controllers.
- Do not use JPA entities as UI models.
- Do not create a custom DI container, event bus, migration runner, i18n framework or mapper framework.
- Do not mix Flyway and Liquibase.
- Do not use DuckDB unless the architecture is explicitly revised later.
- Do not add cloud dependency to the local-first core.
- Do not make agent edits without checkpoint and review.
