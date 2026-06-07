# Project Structure

## Purpose

This file describes the expected future repository structure for Chatly Code.

It is a target architecture, not a prison. During MVP implementation, pragmatic compromises are allowed if they help ship a working vertical slice faster.

Mandatory rule:

```text
Even when modules are temporarily merged, package boundaries and domain boundaries must stay clean.
```

This means the first MVP may start with fewer Gradle modules, but it must not mix UI, domain, persistence, agent runtime, code graph and language-specific analyzer logic into one god module.

## Build Structure

Chatly Code uses Gradle.

Expected root:

```text
chatly-code/
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  gradlew
  gradlew.bat
  build-logic/
  app-desktop/
  app-server/
  agent-server/
  shared-kernel/
  project-domain/
  code-graph/
  language-spi/
  language-java/
  language-rust/
  language-typescript/
  architecture-engine/
  problem-detector/
  task-manager/
  agent-runtime/
  llm-gateway/
  workspace-safety/
  runtime-service/
  sandbox-service/
  git-service/
  conversation-service/
  persistence/
  localization/
  updater/
  mcp-integration/
  docker-integration/
  test-fixtures/
  docs/
  packaging/
```

## MVP Compromise Option

For MVP, this can be reduced to fewer Gradle modules:

```text
chatly-code/
  app-desktop/
  app-core/
  language-spi/
  language-java/
  shared-kernel/
  test-fixtures/
```

Where `app-core` temporarily contains:

```text
project-domain
code-graph
architecture-engine
problem-detector
task-manager
agent-runtime
llm-gateway
workspace-safety
runtime-service
git-service
conversation-service
persistence
localization
updater
```

This is acceptable only if package boundaries are already correct and moving packages into separate Gradle modules later is mechanical.

## Module Responsibilities

### app-desktop

JavaFX desktop shell.

```text
app-desktop/
  src/main/java/com/chatlycode/desktop/
    ChatlyCodeDesktopApplication.java
    DesktopLauncher.java
    bridge/
    view/
    controller/
    viewmodel/
    component/
    dialog/
  src/main/resources/
    fxml/
    css/
    icons/
```

Rules:

- no business logic;
- no repository access;
- no direct file edits;
- no JPA entities;
- ViewModels/DTOs only.

### app-server

Spring Boot application facade for the desktop UI.

```text
app-server/
  src/main/java/com/chatlycode/appserver/
    ChatlyCodeApplication.java
    config/
    facade/
    event/
    lifecycle/
```

Responsibilities:

- application lifecycle;
- UI-facing services;
- event streaming to JavaFX;
- settings orchestration;
- module wiring.

### agent-server

Agent execution boundary.

```text
agent-server/
  src/main/java/com/chatlycode/agentserver/
    AgentServerApplication.java
    api/
    session/
    stream/
    orchestration/
```

MVP may run this in the same JVM as `app-server`, but the boundary must stay visible.

### shared-kernel

Small shared primitives only.

```text
shared-kernel/
  src/main/java/com/chatlycode/shared/
    id/
    time/
    error/
    event/
    result/
```

Rules:

- keep it tiny;
- no domain behavior that belongs elsewhere;
- no Spring-heavy code unless truly shared infrastructure.

### project-domain

Opened project model and scan state.

```text
project-domain/
  src/main/java/com/chatlycode/project/
    domain/
    application/
    port/
    adapter/
```

Owns:

- project registry;
- project scan metadata;
- detected stack;
- build/test command profile;
- source/test roots.

### code-graph

Normalized local code graph.

```text
code-graph/
  src/main/java/com/chatlycode/graph/
    domain/
      CodeFile.java
      CodeNode.java
      CodeEdge.java
      CodeGraph.java
    application/
      CodeGraphIndexer.java
      GraphQueryService.java
      AgentContextBuilder.java
    storage/
      CodeGraphJdbcRepository.java
    query/
      GraphTraverser.java
    sync/
      CodeGraphFileWatcher.java
```

Owns:

- normalized graph model;
- graph queries;
- graph storage;
- graph freshness;
- context builder.

Does not own:

- Java/Spring AST details;
- Rust AST details;
- TypeScript AST details.

### language-spi

Stable language plugin contracts.

```text
language-spi/
  src/main/java/com/chatlycode/language/spi/
    LanguagePlugin.java
    LanguageExtractor.java
    ReferenceResolver.java
    FrameworkResolver.java
    ProblemRule.java
    BuildManifestDetector.java
    TestCommandDetector.java
    SourceRootDetector.java
```

All language plugins depend on this.
Generic core depends on this, not on concrete plugins.

### language-java

Java/Spring plugin.

```text
language-java/
  src/main/java/com/chatlycode/language/java/
    JavaLanguagePlugin.java
    extract/
    resolve/
    spring/
    rules/
    build/
    test/
  src/test/java/
```

Owns:

- Java class/interface/record/method extraction;
- imports/static imports;
- package resolution;
- Spring stereotypes;
- Spring routes;
- Feign/RestClient/WebClient;
- KafkaListener;
- Scheduled;
- Java/Spring problem rules.

Must be extractable into a separate library later.

### language-rust

Rust plugin.

```text
language-rust/
  src/main/java/com/chatlycode/language/rust/
    RustLanguagePlugin.java
    extract/
    resolve/
    cargo/
    rules/
```

Owns:

- Cargo manifest/workspace detection;
- module/use resolution;
- Rust nodes/edges;
- Rust test command detection.

### language-typescript

TypeScript/JavaScript plugin.

```text
language-typescript/
  src/main/java/com/chatlycode/language/typescript/
    TypeScriptLanguagePlugin.java
    extract/
    resolve/
    packagejson/
    tsconfig/
    rules/
```

Owns:

- package.json scripts;
- tsconfig path aliases;
- import/export resolution;
- TS/JS nodes/edges;
- later framework plugins.

### architecture-engine

Architecture and C4 model.

```text
architecture-engine/
  src/main/java/com/chatlycode/architecture/
    domain/
    application/
    c4/
    mermaid/
```

Builds:

- module graph;
- layer model;
- C4 Context;
- C4 Containers;
- C4 Components.

Uses graph evidence. Does not invent structure through LLM.

### problem-detector

Problem detection pipeline.

```text
problem-detector/
  src/main/java/com/chatlycode/problem/
    domain/
    application/
    rule/
```

Runs:

- generic rules;
- registered language plugin rules;
- severity/confidence scoring;
- evidence linking.

### task-manager

Task lifecycle.

```text
task-manager/
  src/main/java/com/chatlycode/task/
    domain/
    application/
    mapper/
```

Owns:

- task creation;
- task status;
- risk;
- definition of done;
- links to problems, files, graph nodes and conversations.

### agent-runtime

Agent loop and tool orchestration.

```text
agent-runtime/
  src/main/java/com/chatlycode/agent/
    domain/
    application/
    prompt/
    tool/
    observation/
    planner/
    reviewer/
```

Uses:

- Spring AI Agent Utils through adapters;
- deterministic context from graph/problem/task services;
- workspace safety for edits;
- runtime service for commands.

### llm-gateway

Spring AI provider abstraction.

```text
llm-gateway/
  src/main/java/com/chatlycode/llm/
    domain/
    application/
    springai/
    provider/
```

Owns:

- model profiles;
- Spring AI `ChatClient`;
- provider routing;
- retries;
- streaming;
- token/cost tracking.

### workspace-safety

Safe workspace mutation.

```text
workspace-safety/
  src/main/java/com/chatlycode/workspace/
    domain/
    application/
    patch/
    policy/
    secret/
```

Owns:

- workspace root enforcement;
- patch application;
- diff collection;
- secret redaction;
- rollback coordination.

### runtime-service

Command execution abstraction.

```text
runtime-service/
  src/main/java/com/chatlycode/runtime/
    domain/
    application/
    process/
    stream/
```

Owns:

- command execution;
- stdout/stderr stream;
- exit code;
- timeouts;
- command observations.

### sandbox-service

Runtime lifecycle.

```text
sandbox-service/
  src/main/java/com/chatlycode/sandbox/
    domain/
    application/
    process/
    docker/
    remote/
```

Owns:

- process runtime lifecycle;
- Docker/Podman runtime lifecycle;
- remote runtime lifecycle later;
- runtime status.

### git-service

Git operations.

```text
git-service/
  src/main/java/com/chatlycode/git/
    domain/
    application/
    cli/
    jgit/
```

Owns:

- status;
- branch;
- checkpoint;
- changed files;
- diff;
- commit;
- rollback.

### conversation-service

Durable conversation and trajectory history.

```text
conversation-service/
  src/main/java/com/chatlycode/conversation/
    domain/
    application/
    event/
```

Owns:

- user messages;
- agent messages;
- tool actions;
- observations;
- conversation/task links.

### persistence

Persistence configuration and migrations.

```text
persistence/
  src/main/java/com/chatlycode/persistence/
    config/
    jpa/
    jdbc/
    transaction/
  src/main/resources/db/changelog/
    db.changelog-master.xml
    product/
    graph/
```

Rules:

- Liquibase XML only;
- JPA/Hibernate for product/domain state;
- Spring JDBC/JdbcClient for high-volume graph writes.

### localization

Spring MessageSource integration.

```text
localization/
  src/main/java/com/chatlycode/i18n/
  src/main/resources/i18n/
    messages.properties
    messages_en.properties
    messages_ru.properties
```

### updater

Background updates.

```text
updater/
  src/main/java/com/chatlycode/update/
    domain/
    application/
    installer/
```

### mcp-integration

MCP adapters.

```text
mcp-integration/
  src/main/java/com/chatlycode/mcp/
    client/
    server/
    tool/
```

### docker-integration

Docker/Podman adapters.

```text
docker-integration/
  src/main/java/com/chatlycode/docker/
    client/
    compose/
```

### test-fixtures

Sample projects and fixtures.

```text
test-fixtures/
  sample-java-spring/
  sample-rust/
  sample-typescript/
  sample-broken-architecture/
```

## Dependency Direction

Allowed direction:

```text
app-desktop
-> app-server
-> application/domain modules
-> ports/SPI
-> adapters
```

Language plugin direction:

```text
code-graph/problem-detector
-> language-spi
<- language-java/language-rust/language-typescript
```

Core must not import plugin internals.

## Gradle Notes

Recommended root settings:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "chatly-code"

include(
    "app-desktop",
    "app-server",
    "agent-server",
    "shared-kernel",
    "project-domain",
    "code-graph",
    "language-spi",
    "language-java",
    "language-rust",
    "language-typescript",
    "architecture-engine",
    "problem-detector",
    "task-manager",
    "agent-runtime",
    "llm-gateway",
    "workspace-safety",
    "runtime-service",
    "sandbox-service",
    "git-service",
    "conversation-service",
    "persistence",
    "localization",
    "updater",
    "mcp-integration",
    "docker-integration",
    "test-fixtures"
)
```

For MVP, fewer includes are allowed if package boundaries remain compatible with this target structure.

## Compromise Rules

Allowed MVP compromises:

- merge several backend modules into `app-core`;
- implement only `language-java` first;
- implement process runtime before Docker/remote;
- generate C4 as Mermaid Markdown before rich UI;
- use CLI git first before JGit if safer/faster;
- keep agent-server in-process first.

Not allowed:

- UI controllers with business logic;
- direct file writes outside `workspace-safety`;
- hardcoded Java/Spring checks in generic graph core;
- language plugins coupled to each other;
- JPA entities in UI or prompts;
- raw LLM calls outside `llm-gateway`;
- migration without Liquibase XML;
- custom coding tools instead of Spring AI Agent Utils without documented reason.

