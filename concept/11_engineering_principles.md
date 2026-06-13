# Engineering Principles

## Product Engineering Values

- Trust over magic.
- Visibility over hidden automation.
- Small safe changes over huge risky diffs.
- Project understanding before code generation.
- Local-first by default.
- Architecture quality as a first-class feature.

## Code Architecture

- JavaFX controllers must be thin.
- Business logic belongs in Spring services.
- Persistence belongs in repositories.
- Domain model should not depend on UI.
- Agent tools must be explicit classes.
- File edits must go through controlled patch APIs.
- Events should be durable and observable.
- Code structure must be derived from AST/static analysis before LLM summarization.
- Graph edges must record provenance and confidence.
- Use package-by-domain and explicit subdomains, not god modules.
- Keep high cohesion inside each domain and low coupling between domains.
- Keep language-specific analysis behind plugin SPI/adapters.
- Generic core must not import Java/Spring/Rust/TypeScript plugin implementation classes.

## Java Practices

- Use Java 25 features where they improve clarity.
- Use records for immutable data carriers.
- Use sealed classes/interfaces for limited hierarchies.
- Use pattern matching and switch expressions when readable.
- Use virtual threads for IO-heavy workloads.
- Use structured concurrency carefully and behind abstraction while preview status matters.
- Use JSpecify annotations and static analysis for null-safety.

## Spring Practices

- Constructor injection only.
- Services should express use cases.
- Keep controllers/adapters thin.
- Separate domain services from infrastructure services.
- Do not leak persistence entities into UI models.
- Use transactions only where needed.
- Use MapStruct for DTO/entity/domain/ViewModel mappings when mapping is non-trivial or repeated.
- Prefer Spring Boot auto-configuration and Spring Framework facilities over custom infrastructure.
- Use Spring Data JPA/Hibernate for normal product/domain persistence.
- Use Spring JDBC/JdbcClient for high-volume graph indexing tables where JPA overhead is not appropriate.
- Use Liquibase with XML changelogs for schema migrations.
- Use Gradle as the mandatory build system for the product.
- Use `@ConfigurationProperties` for typed configuration.
- Use `ApplicationEventPublisher` for normal application events unless a high-volume stream requires a focused implementation.
- Use Micrometer for metrics and SLF4J/Logback for logging.

## JavaFX Practices

- Never block JavaFX Application Thread.
- Use background services for scans, tools, model calls and test runs.
- UI state should be modelled through ViewModels.
- UI should subscribe to domain events.
- Long operations must show progress and cancellation.
- UI text must be loaded through localization keys, never hardcoded.

## Agent Practices

- No edit without plan.
- No edit without checkpoint.
- No hidden commands.
- No repeating failed actions endlessly.
- Always summarize result.
- Always show tests if available.
- Always show diff.

## Testing Strategy

Unit tests:

- domain model;
- task manager;
- problem classification;
- command policy;
- diff parser;
- tool runtime.

Integration tests:

- project scan;
- code graph indexing;
- reference resolution;
- graph queries;
- git checkpoint/rollback;
- agent tool execution;
- SQLite persistence.

UI tests:

- open project;
- task flow;
- approve plan;
- review diff;
- rollback.

## Quality Gates

Before release:

- app starts cleanly;
- project scan works on sample repos;
- no UI freeze during scan;
- rollback works;
- update flow tested;
- secrets redaction tested;
- installer tested on target OS.

## Documentation Rules

Generated architecture docs should be useful outside the app:

- Markdown;
- Mermaid;
- clear file links;
- timestamp/version;
- note that docs are generated.

## Localization Rules

- English is the default language.
- Russian is a required built-in locale.
- All UI text must use Spring `MessageSource` with resource bundles.
- Use `LocaleContextHolder` or an equivalent Spring locale context pattern where locale is needed outside the UI.
- Resource bundles must include `messages.properties`, `messages_en.properties` and `messages_ru.properties`.
- Do not concatenate translated UI strings from fragments when grammar can differ by language.
- Prefer parameterized messages.
- Test screens with Russian text because labels are often longer than English.
- Agent prompts can remain English internally, but user-facing explanations should follow selected language.

## Dependency Rules

- Do not add libraries without clear benefit.
- Prefer mature libraries for diff, git, markdown, SQLite and installers.
- Prefer deterministic parsers/static analysis for code structure.
- Use Spring AI Agent Utils as the default foundation for coding-agent file, shell, grep/glob, todo, skills, memory and subagent tools.
- Keep AI provider integration behind `llm-gateway`.
- Keep Docker/MCP behind interfaces.
- Do not write custom replacements for DI, ORM, migrations, i18n, logging, metrics, validation, mapping or generic HTTP clients when Spring/JVM ecosystem tools cover the need.
- Do not write coding-agent tools from scratch if Spring AI Agent Utils already provides the capability; wrap it with Chatly Code safety, sandbox, event and UI policies.

## Performance Rules

- The JavaFX Application Thread must never run indexing, LLM calls, shell commands, graph queries or file parsing.
- Use virtual threads for IO-heavy concurrent operations.
- Use bounded executors for CPU-heavy parsing and graph work.
- Use backpressure for agent events and tool queues.
- Use streaming UI updates for long-running tasks.
- Use memory limits and batching when indexing large projects.
- Evaluate GraalVM and native-image only where compatibility with JavaFX, Spring Boot, SQLite, tree-sitter and MCP runtime is proven.
- Prefer fast startup and low idle memory for desktop UX.

## Code Graph Rules

- Store files, nodes, edges and unresolved references locally in SQLite.
- Run extraction before LLM analysis.
- Run resolver after extraction.
- Keep unresolved references visible.
- Prefer no edge over wrong edge.
- Framework-aware edges must include provenance.
- Keep graph fresh with content hash and watcher sync.
- Context builder must return bounded, sufficient context for the agent.
- Port CodeGraph's pipeline to Java instead of inventing a new graph architecture without reason.
- Use MapStruct for mapping graph database rows to domain models and UI DTOs where hand-written mapping would become repetitive.
- Implement Java/Spring, Rust and TypeScript-specific analysis as language plugins behind stable core SPI.
- Language plugins must be extractable into separate libraries later.

## Stack Narrative Rules

- The stack should be market-relevant and defensible for public technical review.
- Custom code must be concentrated in product-specific value: code graph, agent orchestration, C4, workspace safety and UX.
- Infrastructure should be boring and standard: Spring, Hibernate/JPA, Spring JDBC, Liquibase XML, Lombok, MapStruct, Micrometer, SLF4J.
- If a custom solution is proposed, document why the standard Spring/JVM option is insufficient.
