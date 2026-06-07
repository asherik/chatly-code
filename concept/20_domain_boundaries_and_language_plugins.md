# Domain Boundaries And Language Plugins

## Purpose

Chatly Code must stay modular enough to grow without turning into a set of god modules.

The product has two different kinds of boundaries:

- product domains and subdomains;
- language/framework-specific analyzer plugins.

Both boundaries must be explicit from the beginning.

## Core Design Principles

- High cohesion inside each domain.
- Low coupling between domains.
- SOLID without pointless abstraction.
- KISS before clever architecture.
- No god services.
- No god files.
- No UI business logic.
- No persistence entities in JavaFX controllers or UI ViewModels.
- No language-specific logic inside generic scanner/graph/task/agent core.

If a class is hard to name, it probably owns too many responsibilities.

## Product Domains

### Project Domain

Owns:

- project registry;
- opened project metadata;
- project scan state;
- detected languages;
- detected build/test commands;
- source/test folders;
- project settings.

Must not own:

- AST parsing;
- graph storage;
- agent execution;
- UI rendering.

### Code Graph Domain

Owns:

- files/nodes/edges;
- unresolved references;
- graph schema;
- graph queries;
- graph freshness;
- impact analysis.

Must not own:

- Java-specific rules directly;
- Rust-specific rules directly;
- problem text wording;
- agent execution.

Language-specific extraction and resolution are plugins behind interfaces.

### Architecture Domain

Owns:

- architecture model;
- C4 model;
- layer model;
- module graph;
- relationships between architecture nodes and code graph evidence.

Must not own:

- parser implementation;
- direct LLM guessing;
- UI rendering.

### Problem Detection Domain

Owns:

- problem rules;
- severity;
- confidence;
- evidence links;
- problem classification;
- rule execution pipeline.

Must not own:

- task execution;
- file edits;
- UI rendering;
- language-specific AST details.

Language/framework-specific problem rules must be plugins.

### Task Domain

Owns:

- task lifecycle;
- task type;
- priority;
- risk;
- definition of done;
- links to problems/files/graph nodes/conversations.

Must not own:

- agent tool execution;
- prompt low-level assembly;
- direct filesystem edits.

### Agent Domain

Owns:

- plan lifecycle;
- action/observation loop;
- agent state;
- tool policy orchestration;
- prompt assembly boundary.

Must not own:

- UI rendering;
- direct file writes;
- direct shell execution;
- graph indexing;
- language-specific inspections.

Agent receives prepared context from deterministic services.

### Workspace Safety Domain

Owns:

- workspace root enforcement;
- git checkpoint/branch;
- diff collection;
- rollback;
- command approval;
- secret redaction;
- file write policy.

No agentic editing may bypass this domain.

### Runtime Domain

Owns:

- process runtime;
- Docker/Podman runtime;
- remote runtime;
- command execution;
- terminal stream;
- timeout/resource policy.

Must not know Java/Rust/TypeScript language rules.

### LLM Gateway Domain

Owns:

- Spring AI integration;
- provider profiles;
- model routing;
- streaming responses;
- token/cost tracking;
- retries.

Must not know UI details, database entities or filesystem mutation details.

### UI Domain

Owns:

- JavaFX views;
- ViewModels;
- interaction state;
- i18n labels;
- user approvals.

Must not own:

- business rules;
- repositories;
- graph indexing;
- agent tool execution;
- persistence entities.

## Recommended Package Boundary Pattern

Use package-by-domain first, not package-by-technical-layer only.

Recommended pattern:

```text
com.chatlycode.project
  application
  domain
  adapter
  port

com.chatlycode.graph
  application
  domain
  query
  storage
  language

com.chatlycode.problem
  application
  domain
  rule
  language

com.chatlycode.agent
  application
  domain
  tool
  prompt

com.chatlycode.workspace
  application
  domain
  git
  policy

com.chatlycode.runtime
  application
  domain
  process
  docker
  remote
```

Avoid generic root packages:

```text
service
manager
helper
utils
common
```

Use them only for tiny, clearly scoped shared helpers.

## Language Plugin Architecture

Language-specific logic must live behind SPI interfaces.

Core must depend on abstractions, not Java/Rust/TypeScript implementations.

Required extension points:

```java
public interface LanguagePlugin {
    String id();
    Set<String> supportedLanguages();
    List<BuildManifestDetector> buildManifestDetectors();
    List<SourceRootDetector> sourceRootDetectors();
    List<LanguageExtractor> extractors();
    List<ReferenceResolver> referenceResolvers();
    List<FrameworkResolver> frameworkResolvers();
    List<ProblemRule> problemRules();
    List<TestCommandDetector> testCommandDetectors();
}
```

Core pipeline:

```text
ProjectScanner
-> LanguagePluginRegistry
-> selected plugins
-> extraction/resolution/problem rules
-> normalized graph/domain model
```

The normalized graph model belongs to Chatly Code core.
Plugin-specific raw data must stay in plugin metadata.

## Ports And Adapters

Use ports/adapters where a boundary protects the core.

Examples:

```text
LanguagePlugin port
JavaLanguagePlugin adapter
RustLanguagePlugin adapter
TypeScriptLanguagePlugin adapter

Runtime port
ProcessRuntime adapter
DockerRuntime adapter
RemoteRuntime adapter

LlmClient port
SpringAiLlmClient adapter

GitClient port
JGit/CliGit adapter
```

Adapters may depend on external libraries.
Domain/core must not depend on adapter implementation details.

## Java/Spring Plugin

Java/Spring-specific checks must live in Java/Spring plugin modules.

Example module:

```text
language-java/
  java-extractor
  java-reference-resolver
  spring-framework-resolver
  spring-problem-rules
```

Java/Spring plugin owns:

- package/import resolution;
- class/interface/record extraction;
- method/field extraction;
- static import resolution;
- annotation extraction;
- Spring stereotype classification;
- route detection;
- Spring dependency injection edges;
- repository/entity/dto/config detection;
- KafkaListener detection;
- Scheduled job detection;
- Feign client detection;
- RestClient/WebClient detection.

Java/Spring plugin problem rules:

- Controller directly uses Repository;
- Entity leaks into API/UI;
- Service depends on UI;
- Repository calls Service;
- missing tests for controller/service;
- god service;
- huge class;
- huge method;
- circular package/module dependency;
- too many dependencies;
- unbounded transaction risk where detectable;
- blocking call in scheduled/async path where detectable.

Core problem detector runs the rules. The Java plugin provides the Java-specific rules.

## Rust Plugin

Rust-specific logic must be isolated in a Rust plugin.

Example module:

```text
language-rust/
  rust-extractor
  rust-reference-resolver
  cargo-workspace-resolver
  rust-problem-rules
```

Rust plugin owns:

- Cargo.toml detection;
- Cargo workspace members;
- module tree;
- function/struct/enum/trait extraction;
- use/import resolution;
- call/reference edges where possible;
- test command detection.

Rust plugin must not depend on Java/Spring plugin.

## TypeScript Plugin

TypeScript/JavaScript-specific logic must be isolated in a TS plugin.

Example module:

```text
language-typescript/
  ts-extractor
  ts-reference-resolver
  package-json-resolver
  framework-resolvers
  ts-problem-rules
```

TypeScript plugin owns:

- package.json detection;
- npm/pnpm/yarn scripts;
- tsconfig path aliases;
- import/export resolution;
- function/class/component extraction;
- framework-specific route/component detection later.

## Extractable Library Rule

Every language plugin must be extractable into a separate library later.

Rules:

- plugin modules depend on core SPI and normalized graph contracts;
- core does not depend on plugin internals;
- no Java plugin class may be referenced from generic graph/problem/task/agent core;
- plugin configuration is loaded through registry;
- plugin outputs normalized nodes/edges/problems;
- plugin-specific metadata is stored as JSON/typed metadata behind stable keys;
- tests for a plugin run without launching the desktop UI;
- plugin packages use stable public interfaces.

Bad:

```text
GraphQueryService imports SpringFrameworkResolver
ProblemDetector imports JavaControllerRule directly
AgentPromptAssembler checks Java annotations manually
```

Good:

```text
GraphQueryService uses normalized graph
ProblemDetector runs ProblemRule from registered plugins
PromptAssembler consumes evidence from graph/problem/task services
```

## Layering Rules

Allowed dependency direction:

```text
UI
-> application services
-> domain services
-> ports
-> adapters
```

Domain must not depend on:

- JavaFX;
- JPA entities;
- Spring MVC/controllers;
- concrete SQLite repositories;
- specific language plugins;
- LLM provider SDKs.

Adapters can depend on:

- Spring;
- SQLite/JDBC/JPA;
- tree-sitter;
- Spring AI Agent Utils;
- Docker clients;
- Git libraries;
- language-specific parsers.

## Data Mapping Rules

Use separate models:

- Domain model;
- Persistence entity;
- DTO;
- ViewModel;
- Graph row model;
- Plugin metadata model.

Rules:

- no JPA Entity in UI;
- no JPA Entity in agent prompts;
- no JavaFX property model in domain;
- MapStruct for repeated mapping;
- direct mapping only for trivial one-off cases;
- graph rows can use Spring JDBC/JdbcClient models for performance.

## Review Checklist

Before accepting implementation:

- module has one clear domain owner;
- no god service;
- no god file;
- UI controllers are thin;
- domain logic is not in adapters;
- language-specific logic is behind plugin SPI;
- Java/Spring checks are not hardcoded in core;
- Rust/TypeScript can be added without changing core APIs;
- persistence entities do not leak to UI/agent;
- dependencies point inward to stable contracts;
- tests exist at domain and adapter levels.

