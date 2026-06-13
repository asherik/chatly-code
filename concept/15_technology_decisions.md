# Technology Decisions

## Goal

Chatly Code should be built as a serious corporate-grade JVM desktop product. The stack must be relevant on the job market and must teach production practices, not custom framework writing.

Rule:

```text
Use Spring/JVM ecosystem capabilities first.
Write custom infrastructure only when the framework does not provide the needed behavior or when performance/security requires a focused implementation.
```

## Core Stack

Required stack:

- Java 25 LTS.
- Spring Boot 4.
- Spring Framework 7.
- Spring AI 2.x or latest stable Spring AI at implementation time.
- JavaFX 26.
- Gradle as the mandatory build system.
- GraalVM JDK as a serious build/runtime option.
- Virtual Threads for IO-heavy work.
- JSpecify null-safety.
- SQLite.
- Tree-sitter or equivalent AST extraction.
- Spring AI Agent Utils as the default foundation for coding-agent tools and subagent orchestration.
- Docker/Podman.
- MCP.
- jpackage + install4j.

## Spring-First Defaults

## Coding Agent Library Decision

Do not implement the coding-agent tool layer from scratch.

Use `spring-ai-agent-utils` from:

```text
C:\projects\chatly-code\examples\spring-ai-agent-utils-main
```

as the default foundation for Spring AI based coding-agent capabilities.

The library provides useful building blocks:

- file system tools;
- shell tools;
- grep/search tools;
- glob tools;
- todo/task tools;
- skills;
- subagent framework;
- memory tools;
- A2A subagent integration.

Rules:

- prefer the library's existing tools before writing our own;
- wrap library tools with Chatly Code policy, approvals, logging and workspace safety;
- route file edits through `workspace-safety`;
- route shell execution through runtime/sandbox policy;
- connect library tool calls to our action/observation event model;
- connect library subagents to our multi-agent coordinator;
- keep code graph, C4, problem detection, UX and review workflow as Chatly Code product value.

Allowed custom code:

- adapters around library tools;
- approval and command policy;
- runtime/sandbox integration;
- graph-aware context injection;
- UI event streaming;
- diff/review/rollback;
- project-specific task classification.

Only write a replacement for a library tool if:

- the library cannot satisfy a required safety or UX constraint;
- performance is insufficient and measured;
- the required behavior is specific to Chatly Code's code graph or review workflow.

## Product Build vs User Project Build

Gradle is mandatory for building Chatly Code itself.

User projects are different. The opened project can use any ecosystem:

- Rust with Cargo;
- Node.js with npm, pnpm or yarn;
- Python with pytest, uv, poetry or tox;
- Go with `go test`;
- Java with Gradle or Maven;
- .NET with `dotnet test`;
- Makefile-based workflows;
- custom scripts.

Chatly Code must detect and store project-specific build/test commands instead of assuming Gradle.

Rules:

- never assume the user project uses Gradle;
- detect build manifests and scripts;
- present detected commands to the user;
- require approval before running risky commands;
- remember approved safe commands per project;
- show command output in the UI;
- include command result in the final task review.

Use Spring for:

- dependency injection;
- configuration;
- profiles;
- lifecycle;
- events;
- validation;
- i18n;
- task execution;
- scheduling;
- resource loading;
- HTTP clients;
- observability integration.

Do not write custom replacements for:

- DI container;
- event bus, unless Spring events are proven insufficient;
- configuration registry;
- i18n registry;
- validation framework;
- migration runner;
- logging facade;
- metrics facade.

## Persistence Decision

Use one local SQLite database family, but choose access style by workload.

### Product/domain state

Use:

- Spring Data JPA;
- Hibernate;
- transactions;
- repositories;
- Liquibase XML migrations.

Good for:

- projects;
- settings;
- tasks;
- conversations;
- tool events;
- model profiles;
- update state;
- user preferences.

### Code graph high-volume data

Use:

- Spring JDBC/JdbcClient;
- prepared statements;
- batch inserts;
- explicit transactions;
- Liquibase XML migrations.

Reason:

Graph indexing writes many rows quickly. For `code_nodes`, `code_edges`, `code_files`, and `code_unresolved_refs`, JPA/Hibernate can be too heavy. This is not "custom persistence"; it is standard Spring JDBC for a performance-sensitive workload.

## Migrations

Use Liquibase with XML changelogs by default.

Rules:

- every schema change is a migration;
- no manual schema mutation in application code;
- migrations are tested;
- graph schema and product schema can be separate migration locations.
- changelog files live in predictable locations, for example:
  - `db/changelog/product/db.changelog-master.xml`;
  - `db/changelog/code-graph/db.changelog-master.xml`;
- changesets must have stable ids and authors;
- destructive changes require explicit rollback strategy;
- startup must fail fast when migrations fail.

## Mapping

Use MapStruct for:

- Entity -> DTO;
- Entity -> ViewModel DTO;
- database row model -> domain model when repetitive;
- API/settings DTOs.

Use manual mapping only when:

- mapping is one-off and trivial;
- mapping needs custom logic that MapStruct would hide;
- performance requires hand-tuned mapping in a hot path.

## Lombok

Use Lombok for Java boilerplate reduction:

- `@Getter`;
- `@Setter` only when mutability is intended;
- `@RequiredArgsConstructor`;
- `@Builder`;
- `@Slf4j`;
- `@Value` where appropriate.

Avoid:

- careless `@Data` on JPA entities;
- Lombok annotations that hide important domain behavior;
- generated `equals/hashCode` on lazy/cyclic entity graphs without explicit design.

## Validation

Use:

- Jakarta Bean Validation;
- Spring validation integration;
- custom validators only for real domain rules.

Validation should exist at boundaries:

- settings forms;
- model profile forms;
- task creation;
- tool configuration;
- MCP configuration;
- update channel configuration.

## Events and Async

Use Spring mechanisms first:

- `ApplicationEventPublisher` for domain/application events where appropriate;
- Spring `TaskExecutor` backed by virtual threads for IO workloads;
- bounded executors for CPU workloads;
- `@Scheduled` or Spring scheduling for background checks where appropriate.

For high-volume agent event streaming, a focused internal event stream may be justified, but it must still integrate cleanly with Spring lifecycle and observability.

## Observability

Use:

- Micrometer for metrics;
- SLF4J + Logback for logs;
- structured log fields where useful;
- local diagnostics by default.

Optional external telemetry must be opt-in.

## HTTP and AI Integration

Use:

- Spring AI for model abstraction;
- Spring `RestClient`/`WebClient` depending on integration needs;
- Spring configuration properties for provider settings.

Do not spread raw HTTP model calls across the codebase. All model access goes through `llm-gateway`.

## Configuration

Use:

- `@ConfigurationProperties`;
- typed settings classes;
- validation annotations;
- profiles for dev/test/prod packaging.

Do not use stringly-typed global maps for core settings.

## Caching

Use Spring Cache abstraction where useful.

Recommended cache implementation:

- Caffeine for in-memory bounded caches.

Rules:

- caches must be bounded;
- cache invalidation must be tied to project scan/sync state;
- no unbounded source-file cache.

## Testing Stack

Use:

- JUnit 5;
- AssertJ;
- Mockito where appropriate;
- Testcontainers where Docker integration needs real containers;
- Spring Boot test slices where useful;
- JavaFX UI test framework to be selected after prototype validation.

## Anti-Bicycle Rules

Do not custom-write:

- ORM;
- DI;
- migration runner;
- i18n system;
- logging facade;
- metrics framework;
- generic mapper framework;
- generic HTTP client abstraction;
- custom JSON library.

Allowed custom code:

- code graph extraction/resolution;
- agent orchestration;
- prompt assembly;
- workspace safety;
- C4 generation from graph evidence;
- UI components specific to the product.

## Public Technical Narrative

If this project is described publicly, the technical story should be defensible:

```text
We use standard Spring Boot/Spring Framework infrastructure for application concerns.
We use Hibernate/JPA where it fits normal domain persistence.
We use Spring JDBC for the high-volume code graph because graph indexing is a write-heavy workload.
We use MapStruct/Lombok/Liquibase XML/Micrometer instead of custom boilerplate frameworks.
The custom part is the product's core value: code graph, agent orchestration, safe edits, C4 and UX.
```
