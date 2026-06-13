# Business Path

## Product Category

Desktop AI development assistant / local AI software engineer / AI maintenance IDE.

Лучшее позиционирование:

```text
AI maintainer for legacy codebases.
```

или:

```text
Local agentic coding workspace for serious codebases.
```

## Главные Jobs To Be Done

### 1. Понять незнакомый проект

Когда разработчик открывает старый проект, он хочет быстро понять:

- где entrypoints;
- какие модули важные;
- где бизнес-логика;
- как запускаются тесты;
- какие зависимости;
- какие места рискованные.

### 2. Передать рутинные задачи AI

Разработчик хочет не писать руками:

- перенос логики между слоями;
- добавление тестов;
- устранение дублирования;
- маленькие багфиксы;
- обновление документации;
- простые рефакторинги.

### 3. Сохранить контроль качества

Разработчик хочет видеть:

- план;
- риски;
- какие файлы будут изменены;
- diff;
- test output;
- возможность rollback.

### 4. Улучшать legacy постепенно

Команда хочет не переписывать старый проект целиком, а улучшать его задачами:

- bug;
- risk;
- improvement;
- tech debt;
- feature.

## Рыночные боли

- AI coding tools часто слишком chat-first.
- Облачные агенты вызывают вопросы безопасности.
- Legacy-проекты сложно объяснить модели.
- Разработчики боятся принимать большие AI-diff.
- Архитектура деградирует от неконтролируемой генерации.
- В командах нет единого AI workflow.

## Product Value

### Для solo developer

- быстрее чинить старый код;
- быстрее добавлять фичи;
- меньше ручной рутины;
- понятнее diff и риски.

### Для команды

- единый процесс AI-разработки;
- меньше хаотичных AI-правок;
- архитектурная дисциплина;
- повторяемый code review flow.

### Для enterprise

- local-first;
- контроль команд;
- audit log;
- ограничения на tools;
- возможность использовать свои модели.

## Монетизация

### Free

- открыть локальный проект;
- базовый scan;
- базовый project overview;
- ограниченный chat;
- ограниченное количество agent tasks.

### Pro

- полный agentic editing;
- C4 Architecture Explorer;
- task board;
- project memory;
- unlimited projects;
- model profiles;
- advanced diff/review;
- background updates.

### Team

- shared rules;
- shared project profiles;
- team policy templates;
- import/export settings;
- advanced audit log.

### Enterprise

- local-only mode enforcement;
- private model gateway;
- command allowlist/denylist;
- MCP policy;
- secrets policy;
- offline license;
- priority support.

## Метрики успеха

- Time to first project insight: меньше 2 минут на средний проект.
- Time to first accepted AI change: меньше 10 минут.
- Accepted diff rate: процент AI-изменений, принятых пользователем.
- Rollback rate: должен снижаться с улучшением агента.
- Test pass rate after agent task.
- Weekly active projects.
- Number of tasks completed per user per week.
- Retention after first successful fix.

## Go-To-Market

Начать с понятного сегмента:

```text
Java/Spring legacy maintenance for individual developers and small teams.
```

Потом расширять:

- Rust;
- JS/TypeScript;
- Python;
- Go;
- enterprise local AI.

## Главный маркетинговый тезис

```text
Stop prompting. Start assigning tasks.
```

На русском:

```text
Не проси нейронку угадать. Ставь ей задачи и принимай проверенный diff.
```

