# Interface Walkthrough

## Цель

Этот файл описывает интерфейс простыми словами. Его задача: чтобы разработчик, дизайнер или AI-агент мог представить продукт в голове и не превратить его в обычный чат с кнопкой "сгенерировать код".

Chatly Code должен ощущаться как локальная desktop-IDE для агентной разработки:

- как OpenHands по агентному циклу;
- лучше для старых проектов за счет локального code graph, C4, задач, классификации проблем и безопасного review-flow;
- понятнее для обычного разработчика, которому надо не просто поговорить с моделью, а реально доработать проект.

## 1. Первый запуск

Пользователь открывает программу.

На экране нет лендинга, рекламы и большого hero-блока. Сразу рабочий стартовый экран:

```text
Chatly Code

Open local project
Recent projects
Model settings
Language: English / Русский
```

Слева маленький список последних проектов. По центру большая рабочая зона с кнопкой `Open local project`. Справа короткая панель статуса:

```text
Model: not configured
Sandbox: local
Updates: up to date
```

Если модель еще не настроена, программа спокойно предлагает добавить provider:

```text
OpenAI-compatible
Anthropic-compatible
Local Ollama
Custom endpoint
```

Пользователь выбирает модель один раз. Потом он просто открывает проект.

## 2. Открытие старого проекта

Пользователь нажимает `Open local project` и выбирает папку старого проекта.

Например:

```text
C:\work\old-crm
```

Программа открывает главное окно.

Верхняя панель:

```text
old-crm | branch: main | model: GPT / local | agent: idle | sandbox: local
```

Слева появляется дерево проекта:

```text
Files
Modules
Architecture
Search
```

В центре пока экран сканирования. Справа пустой inspector, который потом будет показывать детали выбранного файла, проблемы, компонента или задачи.

Снизу есть панель:

```text
Terminal | Tests | Agent Log | Problems
```

Смысл: пользователь сразу видит, что это не чат, а рабочая среда.

## 3. Первичный scan

После открытия проекта программа сама запускает scan.

На центральном экране видно пошаговый прогресс:

```text
Scanning project

Reading file tree
Detecting languages
Detecting build tool
Reading build manifests
Reading git status
Finding tests
Reading README and docs
Building AST
Building code graph
Resolving references
Detecting architecture layers
Finding risks and problems
Preparing C4 draft
Preparing agent context
```

Важно: пользователь видит не "AI thinking", а конкретные технические действия.

В этот момент программа:

- читает файлы;
- определяет языки;
- находит build tool конкретного проекта;
- находит команды build/test/lint конкретного проекта;
- строит AST через tree-sitter или другой parser;
- сохраняет graph в SQLite;
- ищет связи: controller -> service -> repository, module -> module, file -> symbol;
- помечает unresolved references;
- строит первый architecture summary;
- готовит базовые C4-диаграммы;
- создает список найденных проблем.

LLM не должен придумывать структуру проекта. Сначала факты из code graph, потом объяснение человеческим языком.

Важно: Gradle обязателен для сборки самого Chatly Code, но не для проектов пользователя.

Если открыт Rust-проект, программа должна найти что-то вроде:

```text
cargo test
cargo check
```

Если открыт Node.js-проект:

```text
npm test
npm run build
pnpm test
```

Если открыт Python-проект:

```text
pytest
uv run pytest
poetry run pytest
```

Если открыт Java-проект:

```text
./gradlew test
mvn test
```

То есть агент запускает не Gradle по умолчанию, а detected/approved команды конкретного репозитория.

## 4. Главный dashboard после scan

Когда scan завершился, пользователь попадает на `Overview`.

Экран выглядит примерно так:

```text
Project overview

Stack
Java, Spring Boot, Gradle, PostgreSQL

Build
./gradlew test

Architecture
Monolith with web, service and persistence layers.
Main flow: Controller -> Service -> Repository -> Database.

Health
Bugs: 4
Risks: 11
Improvements: 18
Tech debt: 9

Recommended next actions
1. Fix high-risk controller logic
2. Add missing tests around order calculation
3. Split large UserService
```

Слева дерево проекта уже не просто дерево файлов. Там есть режимы:

```text
Files
Modules
Architecture
```

В `Architecture` пользователь видит:

```text
Controllers
Services
Repositories
External Clients
Database
Configuration
Tests
```

То есть старый проект становится понятным не только как куча файлов, а как система.

## 5. Вкладка Architecture

Пользователь нажимает `Architecture`.

В центре открывается карта проекта:

```text
Web Layer
  UserController
  OrderController

Application Layer
  UserService
  OrderService

Persistence Layer
  UserRepository
  OrderRepository

External
  PaymentClient
  EmailClient

Database
  PostgreSQL
```

Это может быть интерактивная graph-view или аккуратная C4/Mermaid view.

Пользователь кликает `OrderController`.

Справа inspector показывает:

```text
OrderController

Responsibilities detected:
- accepts order HTTP requests
- validates request
- calculates discount
- checks order status

Files:
src/main/java/.../OrderController.java

Depends on:
OrderService
OrderRepository

Problems:
1 high-risk issue
2 improvements
```

Главное: пользователь понимает, что этот controller делает слишком много.

## 6. C4-диаграммы

Пользователь нажимает вкладку `C4`.

Сверху переключатель:

```text
Context | Containers | Components | Code
```

На `Context` видно:

```text
User -> old-crm -> PostgreSQL
old-crm -> Payment Gateway
old-crm -> Email Provider
```

На `Containers` видно:

```text
Desktop/browser client
Spring Boot application
PostgreSQL database
External payment API
```

На `Components` видно:

```text
OrderController -> OrderService -> OrderRepository -> Database
OrderService -> PaymentClient -> Payment Gateway
```

Если связь точная, она помечается как evidence-backed. Если программа не уверена, связь помечается как inferred.

Пользователь может нажать на связь и увидеть:

```text
Evidence:
OrderService.java:42 calls PaymentClient.charge(...)
```

Так C4 не выглядит как красивая фантазия. Это объяснение проекта по фактам.

## 7. Вкладка Problems

Пользователь нажимает `Problems`.

Проблемы не лежат общей кучей. Они разделены:

```text
Bugs
Risks
Tech Debt
Improvements
Feature Ideas
```

Карточка проблемы выглядит так:

```text
Risk / High
OrderController contains business logic

Why it matters:
Controller mixes HTTP layer and domain rules. Tests become harder and changes become risky.

Evidence:
OrderController.java:57
OrderController.java:81

Suggested task:
Move discount and order status rules to OrderService and add tests.

Actions:
Open
Create task
Ignore
```

Пользователь не обязан сам формулировать задачу. Программа уже предлагает нормальную инженерную задачу.

## 8. Создание задачи из проблемы

Пользователь нажимает `Create task`.

Открывается задача:

```text
Task: Move order business logic out of OrderController

Type: Improvement
Risk: Medium
Source: detected problem

Goal:
Move discount calculation and status validation into OrderService.

Definition of done:
- controller only handles request/response flow;
- business rules live in service;
- tests cover discount and status rules;
- detected project test command passes.
```

Кнопки:

```text
Generate plan
Run agent
Edit task
Defer
```

Если пользователь новичок, он просто нажимает `Generate plan`.

Если пользователь опытный, он может вручную поправить цель и definition of done.

## 9. Создание своей фичи

Пользователь хочет не только чинить старое, но и добавить новую возможность.

Он нажимает кнопку `New task`.

Открывается форма:

```text
What do you want to change?

[ Add export of monthly orders to CSV ]

Type:
Feature / Bug / Refactor / Test / Docs / Investigation

Scope:
Whole project / Selected module / Selected files
```

Пользователь пишет простыми словами:

```text
Добавь экспорт заказов за месяц в CSV.
```

Программа не сразу пишет код. Сначала она уточняет задачу через graph:

```text
I found order-related code:
- OrderController
- OrderService
- OrderRepository
- OrderDto
- OrderServiceTest

Likely implementation area:
- web endpoint or UI action
- service method for monthly order query
- CSV export utility
- tests
```

Потом предлагает план:

```text
Plan draft

1. Inspect existing order query methods.
2. Add service method for monthly order export.
3. Add CSV formatter.
4. Add controller endpoint or command according to project style.
5. Add tests.
6. Run detected project test command.
```

Пользователь нажимает `Approve`.

## 10. Agent screen

Когда агент работает, пользователь видит не магический чат, а понятный журнал.

Центр экрана:

```text
Agent run: Move order business logic out of OrderController

Step 1: Understanding relevant files
Step 2: Planning changes
Step 3: Waiting for approval
Step 4: Applying patch
Step 5: Running tests
Step 6: Preparing review
```

Ниже action log:

```text
Read: OrderController.java
Read: OrderService.java
Read: OrderServiceTest.java
Search: discount calculation
Patch: OrderController.java
Patch: OrderService.java
Patch: OrderServiceTest.java
Command: detected project test command
```

Каждый пункт раскрывается. Пользователь может посмотреть, что именно агент прочитал и почему.

Если агент хочет выполнить опасную команду, появляется confirmation:

```text
Agent wants to run:
detected project test command

Reason:
Verify changes after moving business logic.

Allow once
Always allow this safe test command for this project
Reject
```

## 11. Multi-agent режим

Для сложной задачи программа может включить несколько ролей, но пользователь не должен видеть хаос.

На экране это выглядит так:

```text
Coordinator
Owns task and final plan

Architect
Checks project structure and risks

Implementer
Prepares patch

Reviewer
Checks diff and missing tests

Tester
Runs build/test commands
```

Пользователь видит компактный статус:

```text
Architect: done
Implementer: editing
Reviewer: waiting
Tester: waiting
```

Важно: только один агент пишет файлы. Остальные читают, анализируют и дают выводы. Это защищает проект от конфликтов.

## 12. Diff review

Когда агент закончил, автоматически открывается `Diff`.

Слева список файлов:

```text
OrderController.java
OrderService.java
OrderServiceTest.java
```

В центре side-by-side diff.

Справа summary:

```text
What changed:
- discount logic moved to OrderService;
- controller now delegates to service;
- added tests for discount cases.

Tests:
detected project test command
146 passed

Risk after changes:
Low
```

Кнопки:

```text
Accept changes
Request revision
Rollback
Create commit
```

Пользователь может принять все, попросить доработку или откатить.

## 13. Если тесты упали

Если test command упал, программа не прячет это в терминале.

Открывается понятный экран:

```text
Tests failed

Failed test:
OrderServiceTest.shouldCalculateVipDiscount

Reason:
Expected 10%, actual 5%.

Likely cause:
Existing VIP rule was not preserved during refactor.

Actions:
Ask agent to fix
Open failed test
Rollback
```

Пользователь нажимает `Ask agent to fix`, и агент получает новую подзадачу с конкретным failure context.

## 14. Knowledge и память проекта

После нескольких задач программа накапливает знание проекта.

Во вкладке `Knowledge` есть:

```text
Architecture notes
Project rules
Accepted decisions
Generated C4 docs
Known risky files
Completed tasks
Ignored problems
```

Пример decision:

```text
Decision:
Business rules must stay in service layer, not controllers.

Accepted after task:
T-104 Move order business logic out of OrderController.
```

Следующие агенты используют это как контекст и не повторяют старые ошибки.

## 15. Как пользователь перестает руками писать код

Обычный рабочий день выглядит так:

```text
Open project
Review detected problems
Pick one task or create feature task
Generate plan
Approve plan
Watch agent execution
Review diff
Run tests
Accept or rollback
Create commit
```

Пользователь больше не пишет руками каждую строку. Он работает как инженер-руководитель изменений:

- формулирует цель;
- смотрит план;
- контролирует риски;
- принимает diff;
- не теряет понимание архитектуры.

## 16. Почему это должно быть не хуже OpenHands

OpenHands силен тем, что дает агенту tools, shell, file editing, browser/runtime loop и рабочий цикл "plan -> act -> observe".

Chatly Code должен взять это ядро, но усилить desktop-продуктом:

- локальное приложение, не зависящее от web-сервера;
- project understanding перед coding;
- deterministic code graph до LLM;
- C4 architecture из фактов проекта;
- problem/task board вместо хаотичного чата;
- явная классификация: bug, risk, improvement, tech debt, feature;
- безопасный git checkpoint до edits;
- обязательный diff review;
- target-project build/test flow;
- rollback как нормальная кнопка;
- мультиязычный UI;
- локальная история решений проекта.

Итог: пользователь не просто просит AI "сделай фичу", а ведет разработку через управляемый engineering workflow.

OpenHands-like execution loop for Chatly Code:

```text
User task
-> project context
-> agent plan
-> user approval
-> tool action
-> observation from file system / terminal / graph
-> patch
-> live diff
-> detected target-project test/build command
-> test/build output observation
-> reviewer summary
-> accept, request revision or rollback
```

This loop is not Gradle-specific. Gradle is only the build system for Chatly Code itself. The target project decides the command:

```text
Rust: cargo test
Node.js: npm test / pnpm test
Python: pytest / uv run pytest
Go: go test ./...
Java Gradle: ./gradlew test
Java Maven: mvn test
```

## 17. Главное ощущение интерфейса

Интерфейс должен ощущаться так:

```text
Я открыл старый проект.
Программа сама поняла структуру.
Она показала архитектуру.
Она нашла реальные проблемы.
Она отличила баг от улучшения.
Я выбрал задачу или написал новую фичу.
Агенты составили план.
Я одобрил.
Они внесли изменения.
Тесты запустились.
Я увидел diff.
Я принял или откатил.
Проект стал лучше, а я не потерял контроль.
```
