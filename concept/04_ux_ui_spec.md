# UX/UI Specification

## UX Principle

Приложение должно ощущаться как рабочий инструмент разработчика, а не как лендинг или игрушечный чат.

Главный UX:

```text
Проект -> Архитектура -> Проблемы -> Задачи -> План -> Изменения -> Проверка -> Принятие
```

## Layout

Основной layout:

```text
+--------------------------------------------------------------------------------+
| Top Bar: Project / Branch / Model / Agent Status / Update Status               |
+----------------------+----------------------------------+----------------------+
| Project Explorer     | Main Workspace                   | Inspector            |
|                      |                                  |                      |
| files/modules        | Overview / Agent / C4 / Diff     | Problems / Task      |
| architecture nodes   |                                  | Details / Risk       |
| symbols              |                                  |                      |
+----------------------+----------------------------------+----------------------+
| Bottom Panel: Terminal / Test Output / Agent Log / Problems                    |
+--------------------------------------------------------------------------------+
```

## Top Bar

Содержит:

- название проекта;
- текущую git branch;
- выбранную модель;
- статус агента;
- статус sandbox;
- update indicator;
- settings.

Пример:

```text
old-crm | main | GPT-4.1 / Local Ollama | Agent: idle | Sandbox: local | Update ready
```

## Left Sidebar

Tabs:

- Files.
- Modules.
- Architecture.
- Search.

Files:

```text
src
  main
    java
  test
build.gradle
README.md
```

Modules:

```text
api
domain
persistence
web
```

Architecture:

```text
Controllers
Services
Repositories
External Clients
Database
```

## Main Workspace

Tabs:

```text
Overview
Conversation
Agent
Terminal
C4
Problems
Tasks
Plan
Git Changes
Diff
Knowledge
Settings
```

### Overview

Показывает:

- stack;
- project summary;
- health score;
- detected commands;
- recent tasks;
- architecture summary.

Карточки должны быть плотными, без marketing hero.

### Agent

Не просто чат, а рабочий журнал:

```text
User: Fix task T-104
Agent: I will inspect files first.
Tool: Search "OrderController"
Observation: 3 matches
Tool: Read OrderController.java
Agent: Found business logic in controller.
```

Каждое действие collapsible.

### Conversation

Direct OpenHands-like task flow:

```text
User: Add monthly CSV export for orders.
Agent: I will inspect the project and propose a plan.
Tool: Graph search order export
Observation: OrderService, OrderRepository and OrderController are relevant.
Agent: Plan ready. Waiting for approval.
```

Conversation must not hide tool actions. Tool calls, terminal output, file changes and observations must be linked from the conversation.

### Terminal

Shows runtime command output.

Required:

- process/docker/remote runtime status;
- running command;
- stdout/stderr stream;
- exit code;
- command duration;
- stop command action where policy allows it.

Agent commands are read-only by default. Interactive terminal is a separate explicit mode.

### C4

Вкладки:

```text
Context
Containers
Components
Code
```

Справа inspector выбранного узла.

Клик по узлу:

- files;
- dependencies;
- incoming/outgoing relations;
- problems;
- tasks;
- generate task button.

### Plan

Показывает текущий план агента:

```text
1. Inspect OrderController
2. Move rules to OrderService
3. Add tests
4. Run build
```

Пользователь может:

- approve;
- edit;
- reject;
- request smaller plan.

### Diff

Должен быть центральной частью доверия.

Нужно:

- list of changed files;
- side-by-side diff;
- inline comments later;
- accept/reject file;
- accept all;
- rollback.

### Git Changes

Shows repository state:

- current branch;
- changed files;
- staged/unstaged status;
- checkpoint;
- file-level diff;
- create commit action;
- rollback action.

### Knowledge

Показывает:

- project rules;
- architecture notes;
- generated summaries;
- C4 docs;
- decisions.

## Right Inspector

Контекстно меняется.

Для задачи:

```text
Task T-104
Type: Improvement
Severity: Medium
Confidence: High
Status: Planning

Files:
- OrderController.java
- OrderService.java

Actions:
[Run Agent]
[Edit Task]
[Ignore]
```

Для проблемы:

```text
Problem
Type: Bug
Evidence:
UserService.java:88

Why it matters:
...

[Create Task]
```

Для C4 node:

```text
Component: OrderService
Responsibilities:
- order validation
- discount calculation

Depends on:
- OrderRepository
- PaymentClient

Problems:
2
```

## Bottom Panel

Tabs:

- Terminal.
- Tests.
- Agent Log.
- Problems.

Terminal должен быть read-only по умолчанию для agent commands, но пользователь может открыть interactive terminal отдельно.

## Visual Style

- Плотный professional UI.
- Минимум декоративных элементов.
- Карточки только для повторяемых сущностей: tasks, problems, changed files.
- Цвета статусов должны быть функциональными:
  - red: bug/high risk;
  - amber: warning/tech debt;
  - blue: improvement;
  - green: done/pass;
  - gray: neutral.
- Не использовать огромные hero-блоки.
- Не использовать декоративные gradient blobs.
- Текст должен помещаться в кнопки и панели.

## Localization UX

The UI must be multilingual from the first commercial version.

Required languages:

- English, default;
- Russian.

Rules:

- Do not hardcode user-facing strings in JavaFX controllers or views.
- Every label, button, menu, tooltip, dialog and empty state must use Spring-backed i18n keys.
- Use Spring `MessageSource` and resource bundles as the canonical corporate localization approach.
- User can change language in Settings.
- Language switch should not require reinstall.
- If possible, language switch should apply without full app restart.
- Agent technical prompts may stay English internally for model quality.
- User-facing agent summaries should be rendered in the selected UI language.
- Layout must handle longer Russian strings without clipping.
- Buttons must have stable min widths and responsive text wrapping where needed.

Settings example:

```text
Language: English / Русский
Default: English
```

## Empty States

После первого запуска:

```text
Open a local project to scan architecture, detect tasks, and start safe agentic coding.
```

Если проблем нет:

```text
No critical problems found. You can run a deeper analysis or create a custom task.
```

Если нет модели:

```text
No model configured.
Add OpenAI, Anthropic-compatible, or local Ollama model.
```

## Critical UX Rules

- Перед изменениями всегда показывать план.
- Перед опасной shell-командой всегда confirmation.
- После изменений всегда показывать diff.
- После failed tests показывать причину и действия.
- Пользователь должен понимать, что агент делает прямо сейчас.
- Никогда не скрывать file changes.
- Rollback должен быть очевидным.
