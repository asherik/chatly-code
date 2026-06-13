# Roadmap

## Phase 0: Product Prototype

Goal: prove UX and project scan value.

Scope:

- JavaFX shell.
- Open local project.
- Scan file tree.
- Detect languages/build tools.
- Create initial SQLite code graph tables.
- Index Java files into files/nodes/edges.
- Show overview.
- Basic model settings.
- i18n foundation with English default and Russian locale.
- basic performance diagnostics for startup, scan time and UI freeze warnings.
- Basic chat with project context.

Exit criteria:

- user can open a real project;
- app explains stack and structure;
- user sees enough value before agent edits code.

## Phase 1: Agent MVP

Goal: safe single-task agent.

Scope:

- task creation;
- plan generation;
- approval flow;
- read/search tools;
- apply patch tool;
- shell test command;
- git checkpoint;
- diff viewer;
- rollback.

Exit criteria:

- user can select a task;
- agent plans;
- user approves;
- agent edits;
- tests run;
- diff is reviewable;
- rollback works.

## Phase 2: Project Doctor

Goal: product starts creating value without user writing prompts.

Scope:

- CodeGraph-style graph search/explore;
- reference resolver for Java imports/packages;
- Spring framework resolver for controllers/routes/services/repositories;
- problem detection;
- task board;
- bug/risk/improvement/tech debt/feature classification;
- severity/confidence;
- affected files;
- evidence;
- suggested fixes.

Exit criteria:

- old project produces useful task list;
- user can distinguish bug from improvement;
- tasks are actionable.

## Phase 3: Architecture Explorer

Goal: serious differentiation.

Scope:

- C4 generation from code graph evidence;
- module graph;
- dependency graph;
- interactive C4 UI;
- architecture smells;
- generated docs/architecture files.

Exit criteria:

- user can understand project architecture visually;
- C4 node links to files/problems/tasks;
- generated diagrams are useful enough to keep in repo.

## Phase 4: Strong Runtime

Goal: closer to OpenHands-level agent capabilities.

Scope:

- Docker/Podman sandbox;
- MCP client;
- internal MCP server exposing code graph tools;
- task queue;
- multi-step recovery;
- richer tools;
- local memory;
- model profiles by task type.
- virtual-thread based IO runtime with bounded CPU parse workers.

Exit criteria:

- agent can handle larger tasks safely;
- runtime can isolate risky execution;
- MCP tools extend capabilities.
- graph context reduces manual file reads.
- multi-agent execution respects concurrency limits and file-write serialization.

## Phase 5: Commercial Desktop

Goal: production-ready paid product.

Scope:

- installer;
- signed builds;
- background updates;
- crash reporting optional;
- license system;
- onboarding;
- docs;
- polished settings;
- complete English/Russian UI localization;
- performance budget validation;
- release channels.

Exit criteria:

- users can install/update without developer setup;
- app feels reliable;
- payment/licensing path exists.

## Phase 6: Team / Enterprise

Scope:

- shared project rules;
- policy engine;
- audit logs;
- private model gateway;
- command allowlist;
- offline activation;
- team settings import/export.

## Release Strategy

### Alpha

Target: technical users.

Focus:

- project scan;
- task board;
- safe agent.

### Beta

Target: early paying users.

Focus:

- C4;
- better UX;
- installers;
- update flow.

### 1.0

Target: serious market.

Must have:

- stable scan;
- stable agent flow;
- diff/rollback;
- tests;
- C4;
- installer;
- updates;
- English and Russian localization;
- model settings;
- performance targets met;
- documentation.
