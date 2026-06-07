# Chatly Code Concept

Документы в этой папке описывают концепт локального desktop-продукта для агентной разработки.

Продуктовая идея: локальная программа для разработчиков, которая открывает существующий проект, понимает его архитектуру, находит проблемы, создает задачи, строит C4-диаграммы и безопасно запускает AI-агента для исправлений через план, git-checkpoint, тесты и diff-review.

## Структура документов

- [01_product_vision.md](01_product_vision.md) - позиционирование, аудитория, ценность, отличие от OpenHands.
- [02_business_path.md](02_business_path.md) - бизнес-путь продукта, рынок, монетизация, метрики.
- [03_customer_journey.md](03_customer_journey.md) - пользовательский путь от первого запуска до регулярной работы.
- [04_ux_ui_spec.md](04_ux_ui_spec.md) - интерфейс, экраны, навигация, UX-принципы.
- [05_c4_architecture.md](05_c4_architecture.md) - C4-модель продукта: Context, Containers, Components, Code.
- [06_system_architecture.md](06_system_architecture.md) - внутренняя архитектура модулей.
- [07_agent_runtime.md](07_agent_runtime.md) - агентный цикл, tools, события, состояния задач.
- [08_domain_model.md](08_domain_model.md) - ключевые доменные сущности.
- [09_security_safety.md](09_security_safety.md) - безопасность, sandbox, approvals, secrets, rollback.
- [10_roadmap.md](10_roadmap.md) - MVP, версии, коммерческий roadmap.
- [11_engineering_principles.md](11_engineering_principles.md) - инженерные принципы и best practices.
- [12_updates_distribution.md](12_updates_distribution.md) - упаковка, обновления, installer, release flow.
- [13_code_graph_engine.md](13_code_graph_engine.md) - локальный deterministic code graph по модели CodeGraph: tree-sitter, SQLite, resolver, watcher, MCP/context tools.
- [14_performance_targets.md](14_performance_targets.md) - performance targets, GraalVM, virtual threads, multi-agent concurrency, memory and UI responsiveness.
- [15_technology_decisions.md](15_technology_decisions.md) - corporate stack decisions: Spring-first, Hibernate/JPA where appropriate, Lombok, MapStruct, Liquibase XML, Micrometer, no unnecessary custom frameworks.
- [16_implementation_blueprint.md](16_implementation_blueprint.md) - practical implementation blueprint for AI-assisted development: modules, packages, MVP vertical slice, boundaries and acceptance criteria.
- [17_interface_walkthrough.md](17_interface_walkthrough.md) - simple detailed product walkthrough: first launch, scan, architecture, problems, feature request, agent execution, review and rollback.
- [18_openhands_parity_requirements.md](18_openhands_parity_requirements.md) - mandatory OpenHands-like baseline: file read/edit, command execution, process/docker/remote runtime, action/observation loop, conversation, terminal, git changes/diff and runtime services.
- [19_mvp_scope_checklist.md](19_mvp_scope_checklist.md) - non-negotiable MVP checklist: project scan, code graph, architecture explorer, C4 draft, problems, task generator, graph-aware chat, agent MVP, workspace safety and minimal UI.
- [20_domain_boundaries_and_language_plugins.md](20_domain_boundaries_and_language_plugins.md) - domain/subdomain boundaries, high cohesion/low coupling rules, ports/adapters and extractable language plugin architecture for Java/Rust/TypeScript-specific logic.
- [21_project_structure.md](21_project_structure.md) - target Gradle multi-module project structure, package layout, module responsibilities and allowed MVP compromises.

## Рекомендуемый стек

- Java 25 LTS.
- Spring Boot 4.
- Spring Framework 7.
- Spring AI 2.x или последняя стабильная Spring AI на момент сборки MVP.
- Spring AI Agent Utils as the default foundation for coding-agent tools and subagent orchestration.
- JavaFX 26.
- Gradle as the mandatory build system.
- GraalVM JDK as a serious runtime/build option; native-image is evaluated where compatible.
- Virtual Threads.
- Structured Concurrency там, где статус фичи позволяет использовать ее безопасно.
- JSpecify null-safety.
- SQLite.
- Tree-sitter / AST extraction для локального code graph.
- Docker / Podman.
- MCP.
- jpackage + install4j для production distribution.

## Corporate Development Principle

Use standard Spring/JVM ecosystem mechanisms before writing custom infrastructure:

- Spring Boot auto-configuration instead of manual bootstrapping;
- Spring `MessageSource` for i18n;
- Spring AI Agent Utils for coding-agent tools instead of custom from-scratch tools;
- Spring Data JPA/Hibernate for normal relational domain persistence;
- Spring JDBC/JdbcClient for high-volume graph writes where JPA overhead is not appropriate;
- Liquibase XML for migrations;
- Lombok for safe boilerplate reduction;
- MapStruct for mappings;
- Micrometer for metrics;
- SLF4J/Logback for logging.

## Localization

The product must support multiple UI languages from the beginning:

- default language: English;
- required second language: Russian;
- all user-facing UI text must go through i18n keys;
- Spring `MessageSource` is the canonical localization mechanism;
- generated agent-facing technical prompts can stay English by default;
- user-facing summaries, explanations and labels should follow the selected UI language.

## Главный продуктовый принцип

Пользователь не должен просить AI "попробуй починить проект". Пользователь должен видеть управляемый рабочий процесс:

```text
Открыть проект
-> понять архитектуру
-> увидеть проблемы
-> выбрать задачу
-> проверить план
-> разрешить изменения
-> увидеть diff и тесты
-> принять или откатить
```

## Code Graph Principle

Агент не должен сам бродить по проекту через бесконечные `grep` и чтение файлов. Приложение должно заранее построить локальный deterministic code graph:

```text
files
-> AST extraction
-> nodes / edges / files / unresolved references
-> reference resolver
-> framework-aware edges
-> graph queries
-> compact agent context
```

Этот слой строится без LLM и хранится локально в SQLite.
