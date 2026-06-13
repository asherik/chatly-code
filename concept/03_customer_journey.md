# Customer Journey

## Сценарий: старый проект

Пользователь открывает Chatly Code. У него старый проект, где много хаоса, мало тестов и страшно менять код.

## 1. Первый запуск

Экран:

```text
Chatly Code

[Open Project]
[Recent Projects]
[Model Settings]
```

Пользователь нажимает `Open Project` и выбирает папку.

## 2. Сканирование

Программа показывает понятный progress:

```text
Scanning project

✓ Reading file tree
✓ Detecting languages
✓ Detecting build tools
✓ Reading git status
✓ Finding tests
✓ Reading README/docs
✓ Building dependency map
✓ Detecting architecture layers
✓ Preparing C4 draft
```

Пользователь видит, что программа работает с проектом осознанно, а не просто отправляет все в модель.

## 3. Project Overview

После сканирования открывается overview:

```text
Project: old-crm
Stack: Java, Spring Boot, Gradle, PostgreSQL
Git: clean
Tests: JUnit, 146 tests
Detected test command: ./gradlew test

Health:
4 bugs
11 risks
18 improvements
9 tech debts
```

В центре краткое описание:

```text
Проект похож на монолитное Spring Boot приложение.
Основной поток: REST Controller -> Service -> Repository -> PostgreSQL.
В нескольких контроллерах есть бизнес-логика.
Некоторые Entity используются как API DTO.
```

## 4. C4 Architecture

Пользователь открывает вкладку `C4 Architecture`.

Видит:

- System Context;
- Containers;
- Components;
- Code-level relationships.

Клик по компоненту показывает:

```text
OrderController

Files:
- src/main/java/.../OrderController.java

Depends on:
- OrderService
- OrderRepository

Problems:
- Controller directly uses repository
- Business rule in controller
- No tests for discount calculation
```

## 5. Problems

Во вкладке `Problems` карточки сгруппированы:

```text
Bugs
Risks
Improvements
Tech Debt
Features
```

Карточка:

```text
BUG / High
Null email can crash user creation

Evidence:
UserController.java:42
UserService.java:88

Suggested task:
Validate email before user creation and add test.

[Open] [Create Task] [Ignore]
```

## 6. Task Board

Пользователь выбирает задачу:

```text
Move business logic out of OrderController
```

Экран задачи:

```text
What is wrong:
OrderController calculates discount and checks order status.

Why it matters:
Controller is now mixed with business logic.
This makes code hard to test and easy to break.

Suggested plan:
1. Move discount rules to OrderService.
2. Keep controller as request/response layer.
3. Add OrderService tests.
4. Run detected project tests.
```

Кнопки:

```text
[Generate Plan] [Run Agent] [Defer] [Ignore]
```

## 7. Plan Approval

Агент пишет план:

```text
Plan

1. Read OrderController and OrderService.
2. Locate discount and status rules.
3. Move rules into service methods.
4. Update controller to call service.
5. Add tests for discount cases.
6. Run detected test command.
```

Пользователь видит:

```text
Files likely to change:
- OrderController.java
- OrderService.java
- OrderServiceTest.java

Risk:
Medium
```

Кнопки:

```text
[Approve and create branch] [Edit plan] [Cancel]
```

## 8. Safe Execution

После подтверждения:

```text
Creating checkpoint
Creating branch: agent/order-controller-cleanup
```

Агент работает:

```text
Reading OrderController.java
Reading OrderService.java
Applying patch
Running detected test command
```

Пользователь видит action log, terminal и diff.

## 9. Review

Когда задача завершена:

```text
Done

Changed files:
3

Tests:
146 passed

Risk:
Low after tests
```

Кнопки:

```text
[Accept Changes]
[Request Revision]
[Rollback]
[Create Commit]
```

## 10. Регулярная работа

После нескольких задач проект накапливает память:

- что за архитектура;
- какие правила проекта;
- какие решения уже приняты;
- какие задачи были закрыты;
- какие файлы рискованные.

Пользователь начинает работать не через "напиши мне код", а через:

```text
Покажи проблемы
Сделай план
Исправь задачу
Запусти тесты
Покажи diff
```
