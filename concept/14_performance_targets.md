# Performance Targets

## Goal

Chatly Code must feel like a serious desktop engineering tool: fast startup, responsive UI, predictable memory use, and strong multi-agent throughput without freezing the machine.

Performance is a product feature. If the app feels heavy, users will not trust it with large legacy projects.

## Runtime Strategy

Use:

- Java 25 LTS;
- Virtual Threads for IO-heavy concurrency;
- Structured Concurrency where stable enough and hidden behind abstractions;
- bounded platform-thread pools for CPU-heavy parsing;
- Spring Boot lazy initialization where it improves desktop startup;
- JavaFX background services for long tasks;
- SQLite prepared statements and transactions;
- streaming LLM responses;
- incremental graph indexing.

## GraalVM Strategy

GraalVM must be part of the performance plan, but not blindly forced into fragile packaging.

Use GraalVM for:

- a serious JDK/runtime candidate for production builds;
- benchmarking startup and memory;
- evaluating native-image feasibility;
- testing packaging options;
- possible CLI/helper binaries later.

Native image must be evaluated carefully because the app uses:

- JavaFX;
- Spring Boot;
- Spring AI;
- SQLite;
- tree-sitter/native or WASM parsing;
- MCP;
- dynamic model/provider integrations.

Decision rule:

```text
Use GraalVM JDK/runtime when it improves the production build and remains supportable.
Use GraalVM native-image if it improves startup/memory without breaking JavaFX/Spring/SQLite/tree-sitter packaging.
If native-image creates fragile build/runtime complexity, ship JVM runtime through jpackage/install4j first and keep native-image as an optimization track.
```

## Startup Targets

Cold start target:

```text
App window visible: <= 3 seconds on developer laptop
Interactive shell ready: <= 5 seconds
Recent projects loaded: <= 1 second after window visible
```

Startup rules:

- show UI before loading heavy services;
- defer model provider checks;
- defer project graph opening until project is selected;
- lazy-load MCP integrations;
- lazy-load tree-sitter grammars by language;
- do not block JavaFX Application Thread.

## UI Responsiveness

Targets:

```text
UI frame should not freeze over 100 ms during normal interaction.
Long tasks must show progress within 300 ms.
Cancellation should be visible within 1 second.
```

Rules:

- no indexing on UI thread;
- no database scans on UI thread;
- no LLM calls on UI thread;
- no shell command waiting on UI thread;
- batch UI events;
- collapse noisy agent/tool logs;
- virtualize long lists and logs.

## Project Scan Targets

For a medium project:

```text
Initial file inventory: <= 5 seconds
Basic overview: <= 15 seconds
Usable graph context: <= 60 seconds
Full deep analysis: background task
```

The app should show partial results. User should not wait for full indexing before seeing value.

## Code Graph Performance

Use CodeGraph-inspired indexing:

```text
files -> AST extraction -> SQLite nodes/edges -> resolver -> graph queries
```

Rules:

- content-hash every indexed file;
- skip unchanged files;
- debounce watcher updates;
- index in batches;
- wrap batch writes in transactions;
- use prepared statements;
- add indexes for node kind, name, qualified name, file path, edge source/kind and target/kind;
- keep unresolved references visible;
- use FTS for symbol search if SQLite FTS is available;
- compact/vacuum only when needed and never during active user work.

## Virtual Threads

Use virtual threads for:

- file IO;
- shell process waiting;
- LLM HTTP calls;
- MCP calls;
- git command execution;
- Docker API calls;
- background sync tasks.

Do not use virtual threads as a magic answer for CPU-bound parsing. Parsing should use bounded CPU executors.

## Multi-Agent Runtime

Multi-agent must be controlled, not chaotic.

Agent roles:

- ArchitectAgent;
- PlannerAgent;
- ImplementerAgent;
- ReviewerAgent;
- TestAgent;
- SecurityAgent.

Execution model:

```text
Coordinator
-> creates sub-agent tasks
-> assigns isolated context
-> enforces tool permissions
-> merges results
-> prevents conflicting file edits
```

Rules:

- one writer per file at a time;
- sub-agents can read in parallel;
- edits are serialized through WorkspaceSafety;
- each sub-agent has budget limits;
- each sub-agent emits events;
- Coordinator owns final plan and final diff;
- failed sub-agent result must not poison the whole task silently.

## Concurrency Limits

Default limits:

```text
CPU parse workers: min(availableProcessors, 8)
Concurrent LLM calls: 2 by default
Concurrent shell commands: 1 per task
Concurrent git operations: 1 per project
Concurrent file writers: 1 per project
Concurrent graph reads: many, bounded by SQLite mode
```

These must be configurable for power users later.

## Memory Targets

Idle app:

```text
Target: <= 400 MB after startup
Stretch: <= 250 MB
```

Medium project after indexing:

```text
Target: <= 1.2 GB
```

Rules:

- do not keep full project source in memory;
- store graph in SQLite;
- stream large files;
- cap source snippets;
- release AST trees immediately;
- bound caches with LRU;
- separate UI log retention from persisted event history.

## Agent Event Throughput

Agent events can be noisy. UI must remain readable and fast.

Rules:

- persist full event log;
- render summarized event groups by default;
- collapse repeated tool actions;
- stream terminal output with line limits;
- keep full logs on disk if needed;
- avoid appending unlimited text nodes to JavaFX UI controls.

## LLM Performance

Rules:

- use graph context before raw file context;
- keep prompt context bounded;
- reuse project summaries;
- cache stable project facts;
- stream model output;
- track token usage;
- use smaller/faster model profiles for classification and larger models for planning/implementation when needed.

## Database Performance

SQLite rules:

- use WAL mode where safe;
- use prepared statements;
- use transactions for batch graph writes;
- keep app state and graph schema separated logically;
- create indexes intentionally;
- avoid long write transactions while UI needs reads;
- use migrations from day one.

## Observability

Local diagnostics:

- scan duration;
- indexing files/sec;
- graph node/edge counts;
- LLM latency;
- tool latency;
- memory usage;
- queue depth;
- UI freeze warnings;
- failed command count.

Diagnostics must be local-first. External telemetry must be optional.

## Performance Definition of Done

A feature is not done if:

- it freezes UI;
- it performs unbounded file reads;
- it creates unbounded memory growth;
- it spawns unlimited agents/tasks;
- it makes graph sync stale without warning;
- it has no cancellation path for long work.
