# System Architecture

## High-Level Modules

```text
chatly-code/
  app-desktop/
  app-core/
  app-server/
  agent-server/
  agent-core/
  project-scanner/
  code-graph-engine/
  language-spi/
  language-java/
  language-rust/
  language-typescript/
  architecture-engine/
  problem-detector/
  task-manager/
  tool-runtime/
  sandbox-service/
  runtime-service/
  conversation-service/
  git-service/
  workspace-safety/
  llm-gateway/
  knowledge-store/
  localization/
  performance-monitor/
  updater/
```

## app-desktop

JavaFX UI layer.

Responsibilities:

- render screens;
- bind UI state;
- show agent events;
- show diff/test output;
- collect user approvals;
- never contain business logic;
- never directly edit project files.

Depends on:

- app-core services;
- UI view models.

## app-core

Spring Boot local backend inside desktop process.

Responsibilities:

- application lifecycle;
- dependency injection;
- module coordination;
- event bus;
- settings;
- background jobs;
- local API facade for UI.

## app-server

Spring Boot application boundary used by the JavaFX desktop UI.

Responsibilities:

- expose UI-facing application services;
- manage projects, settings, tasks and conversations;
- stream application events to JavaFX ViewModels;
- coordinate app lifecycle;
- keep desktop UI independent from agent/runtime implementation details.

MVP can run app-server in the same JVM process as JavaFX, but the boundary must stay explicit.

## agent-server

Agent execution boundary.

Responsibilities:

- accept user tasks and conversation messages;
- build graph-backed context;
- call LLM through `llm-gateway`;
- select tool actions;
- process observations;
- maintain agent state;
- stream action/observation events;
- stop repeated failure loops.

MVP can run agent-server in the same JVM process, but it must remain separable from app-server and UI.

## project-scanner

Responsibilities:

- file tree scan;
- language detection;
- build tool detection;
- test command detection;
- git status detection;
- dependency manifest parsing;
- README/docs extraction;
- project summary input.
- ignore rules and generated-file detection;
- source file inventory for code graph indexing.

Output:

- `ProjectScan`;
- `ProjectMap`;
- `DetectedStack`;
- `BuildProfile`;
- `TestProfile`.

## code-graph-engine

Local deterministic code intelligence layer inspired by CodeGraph.

Responsibilities:

- parse source files with tree-sitter or equivalent AST parsers;
- extract code nodes: files, modules, classes, interfaces, methods, functions, variables, routes, components;
- extract direct edges: contains, imports, exports, calls, references, extends, implements, returns, type_of, instantiates, overrides, decorates;
- store graph in SQLite tables;
- store unresolved references during extraction;
- run reference resolver after indexing;
- resolve imports, aliases, package/workspace structure and framework-specific routes;
- synthesize framework-aware edges only when there is clear evidence;
- provide graph queries: callers, callees, impact radius, dependencies, dependents, circular dependencies;
- build compact context for the agent so it does not need to read the whole repository;
- keep graph fresh with file watcher, content hash, debounce sync and connect-time catch-up.

Pipeline:

```text
files
-> ExtractionOrchestrator
-> SQLite nodes/edges/files/unresolved_refs
-> ReferenceResolver
-> GraphQueryService
-> ContextBuilder
-> Agent / C4 / Problem Detector
```

Important rule:

The graph is deterministic and derived from source code. LLM summaries must not be the source of truth for nodes and edges.

## language-spi

Stable extension point for language/framework-specific analysis.

Responsibilities:

- define `LanguagePlugin`;
- define language extractors;
- define reference resolver interfaces;
- define framework resolver interfaces;
- define problem rule interfaces;
- define build/test command detector interfaces;
- expose normalized graph contracts.

Generic core modules depend on `language-spi`, not on concrete language plugins.

## language-java

Java/Spring analyzer plugin.

Responsibilities:

- Java source extraction;
- Java import/package/static import resolution;
- Spring stereotype classification;
- Spring route detection;
- Spring dependency edge detection;
- Java/Spring-specific problem rules.

Must be extractable into a separate library later.

## language-rust

Rust analyzer plugin.

Responsibilities:

- Cargo manifest/workspace detection;
- Rust source extraction;
- module/use resolution;
- Rust-specific problem rules.

Must not depend on Java/Spring plugin.

## language-typescript

TypeScript/JavaScript analyzer plugin.

Responsibilities:

- package.json/script detection;
- tsconfig path alias resolution;
- import/export resolution;
- TS/JS source extraction;
- framework-specific extensions later.

Must not depend on Java/Spring or Rust plugins.

## architecture-engine

Responsibilities:

- module graph;
- dependency graph;
- C4 model;
- architecture smells;
- layer detection;
- architecture documentation generation.

Inputs:

- `ProjectScan`;
- `CodeGraph`;
- source code metadata from code-graph-engine;
- build manifests;
- LLM summaries when needed.

## problem-detector

Responsibilities:

- detect bugs;
- detect risks;
- detect improvements;
- detect tech debt;
- detect missing tests;
- detect architecture violations.

Important rule:

Problem detector must separate evidence from interpretation.

Example:

```text
Evidence:
OrderController imports OrderRepository.

Interpretation:
Controller depends directly on persistence layer.

Problem type:
Architecture Risk / Improvement
```

## task-manager

Responsibilities:

- create tasks from problems;
- track lifecycle;
- assign priority;
- connect tasks to files, C4 nodes and conversations;
- maintain task queue.

## agent-core

Responsibilities:

- planning;
- execution loop;
- action selection;
- observation handling;
- tool approval;
- task completion;
- failure recovery.

Required OpenHands-like loop:

```text
task
-> context
-> plan
-> action
-> observation
-> next action
-> patch
-> diff
-> detected target-project test/build command
-> observation
-> review
```

## tool-runtime

Responsibilities:

- controlled file read;
- controlled search;
- controlled edit;
- shell commands;
- git commands;
- test/build commands;
- Docker runtime;
- MCP calls.

All tool calls must emit events.

## sandbox-service

Responsibilities:

- create process runtime;
- create Docker/Podman runtime;
- connect to remote runtime later;
- report runtime status;
- start, stop, pause and resume runtime;
- enforce command policy;
- stream terminal output;
- clean resources.

Runtime modes:

- process;
- docker;
- remote.

## runtime-service

Responsibilities:

- execute approved commands;
- manage command timeouts;
- capture stdout/stderr;
- expose terminal streams;
- map command result to observations;
- keep runtime details out of agent-core.

## conversation-service

Responsibilities:

- persist conversations;
- store user messages;
- store agent messages;
- store action/observation trajectory;
- connect conversation to project, task, files and git changes.

## git-service

Responsibilities:

- git status;
- branch creation;
- checkpoint;
- changed files;
- diff;
- commit;
- rollback through safe APIs.

## workspace-safety

Responsibilities:

- git branch creation;
- checkpoint creation;
- dirty worktree checks;
- rollback;
- diff collection;
- dangerous command policy;
- file write policy.

## llm-gateway

Responsibilities:

- Spring AI integration;
- model profiles;
- provider routing;
- prompt templates;
- token/cost tracking;
- retries;
- streaming responses;
- embeddings.

Must support:

- OpenAI-compatible APIs;
- local Ollama;
- later Anthropic-compatible providers if Spring AI supports target model.

## knowledge-store

Responsibilities:

- SQLite persistence;
- project scans;
- code graph tables;
- architecture model;
- tasks;
- conversations;
- tool events;
- file changes;
- test runs;
- generated docs;
- summaries.

## localization

Spring-style localization module.

Responsibilities:

- configure `MessageSource`;
- load `messages.properties`, `messages_en.properties`, `messages_ru.properties`;
- expose locale-aware message service to JavaFX ViewModels;
- store selected language in settings;
- use English as default locale;
- use Russian as required built-in locale;
- keep all user-facing strings behind message keys.

Important rule:

Localization must follow standard Spring corporate patterns. Avoid custom ad-hoc translation registries unless they wrap Spring `MessageSource`.

## performance-monitor

Responsibilities:

- track UI responsiveness;
- track project scan time;
- track graph indexing throughput;
- track agent task latency;
- track memory usage;
- track tool queue depth;
- expose diagnostics for support;
- collect local-only metrics unless user explicitly enables telemetry.

## updater

Responsibilities:

- check update metadata;
- download update in background;
- verify signature/hash;
- prompt restart;
- hand off to installer/updater.

## Communication Pattern

UI should subscribe to application events:

```text
ProjectScanStarted
ProjectScanProgress
ProblemDetected
TaskCreated
PlanCreated
ToolActionStarted
ToolActionFinished
FileChanged
TestsFinished
AgentNeedsReview
```

This keeps UI reactive and avoids blocking JavaFX Application Thread.
