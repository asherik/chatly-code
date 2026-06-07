# OpenHands Parity Requirements

## Goal

Chatly Code must not be only an architecture scanner or a pretty AI chat. It must include the core agentic development capabilities that make OpenHands useful:

```text
read files
edit files
run commands
observe results
continue work
show terminal output
show git changes/diff
let the user review and decide
```

The product may differ in UX and architecture, but this baseline is mandatory.

## Required Agent Capabilities

The agent must be able to:

- read files with size limits;
- list files with ignore rules;
- search code;
- inspect the code graph;
- inspect git status;
- create safe patches;
- edit files through controlled APIs;
- run approved shell commands;
- run detected project build/test/lint commands;
- inspect command output;
- inspect git changes and file diffs;
- ask for approval when policy requires it;
- recover from failed commands without repeating the same failed action endlessly.

The agent must not directly mutate files outside `workspace-safety`.

Implementation rule:

```text
Do not implement these coding-agent tools from scratch.
Use Spring AI Agent Utils first.
```

Reference:

```text
C:\projects\chatly-code\examples\spring-ai-agent-utils-main
```

Use/adapt its FileSystemTools, ShellTools, GrepTool, GlobTool, TodoWriteTool, SkillsTool, TaskTools, subagent framework, memory tools and A2A support. Chatly Code adds policy, sandbox, graph context, durable events, UI and review workflow around those tools.

## Sandbox And Runtime

The product must support runtime modes as first-class architecture, not as a UI detail.

Required runtime modes:

```text
process
docker
remote
```

### process runtime

Runs commands on the local machine with strict command policy.

Good for:

- MVP;
- trusted local projects;
- fast feedback;
- simple desktop setup.

Risks:

- weaker isolation;
- command policy must be strict;
- secrets and environment filtering matter.

### docker runtime

Runs commands inside Docker/Podman with mounted workspace and resource limits.

Good for:

- risky projects;
- dependency installs;
- unknown scripts;
- reproducible execution.

Required controls:

- workspace mount policy;
- network policy;
- environment filtering;
- CPU/memory limits;
- container cleanup;
- command timeout.

### remote runtime

Runs agent commands through a remote runtime service.

Good for:

- heavy projects;
- team/company setups;
- machines without local Docker;
- future cloud/enterprise edition.

Required controls:

- authentication;
- runtime status;
- log streaming;
- workspace sync policy;
- secret redaction;
- explicit user consent.

## Action / Observation Loop

The core agent loop must be event-based:

```text
Task
-> Context
-> Plan
-> Action
-> Observation
-> Next action
-> Patch
-> Diff
-> Test/build command
-> Observation
-> Review
-> Accept / revise / rollback
```

Actions are what the agent tries to do.
Observations are what actually happened.

Examples:

```text
Action: read file src/main.rs
Observation: file content loaded, 184 lines

Action: run cargo test
Observation: 2 tests failed, output captured

Action: apply patch
Observation: 3 files changed, diff available
```

Rules:

- every action must create a durable event;
- every observation must be stored;
- UI must show action and observation history;
- agent prompts must receive recent relevant observations;
- failed observations must influence the next action;
- repeated failure loops must be stopped.

## UI Parity

The desktop UI must include these OpenHands-like working surfaces:

- conversation view;
- agent action log;
- terminal;
- tabs;
- git changes;
- file diff;
- runtime status;
- task state;
- settings;
- model/provider settings.

Recommended main tabs:

```text
Overview
Conversation
Agent Run
Terminal
C4
Problems
Tasks
Git Changes
Diff
Knowledge
Settings
```

Conversation is not the whole product, but it must exist. Some users want the direct "give task, agent works" flow.

## Service Architecture Parity

The product must separate responsibilities similarly to OpenHands concepts, adapted to Java/Spring/JavaFX.

Required services:

```text
Desktop UI
App Server
Agent Server
Sandbox Service
Runtime Service
Workspace Service
Git Service
Conversation Service
Settings Service
LLM Gateway
Code Graph Service
Task Service
Tool Service
```

### App Server

Spring Boot local application backend.

Responsibilities:

- app lifecycle;
- settings;
- project registry;
- task registry;
- conversation registry;
- UI-facing application facade;
- event streaming to JavaFX ViewModels.

### Agent Server

Agent execution boundary.

Responsibilities:

- receive user task;
- build context;
- call LLM through `llm-gateway`;
- select actions;
- call tools;
- process observations;
- maintain agent state;
- stream events.

In MVP it may run in the same JVM process, but it must be architecturally separated.

### Sandbox Service

Runtime lifecycle boundary.

Responsibilities:

- create process/docker/remote runtimes;
- report runtime status;
- start/stop/pause/resume runtime;
- execute commands through policy;
- stream terminal output;
- clean resources.

### Workspace Service

Controls project files.

Responsibilities:

- read files;
- apply patches;
- enforce workspace root;
- protect ignored/secret files;
- coordinate file locks;
- collect diff.

### Git Service

Responsibilities:

- status;
- branch;
- checkpoint;
- diff;
- changed files;
- commit;
- rollback through safe APIs.

## Integrations

The product must integrate with:

- local repositories;
- Git status and branches;
- GitHub/GitLab later;
- model providers through Spring AI;
- local Ollama;
- OpenAI-compatible endpoints;
- MCP tools;
- Docker/Podman;
- settings profiles;
- runtime status.

Repository integrations should not be required for local-first MVP, but the architecture must not block them.

## "Give Task And Agent Works" Flow

The product must support a direct OpenHands-like flow:

```text
Open project
-> type task
-> agent inspects project
-> agent plans
-> user approves
-> agent reads files
-> agent edits files
-> agent runs commands
-> agent observes results
-> agent fixes failures if needed
-> user reviews diff
-> user accepts or rolls back
```

This flow must work even if the user ignores C4, Problems and Task Board.

The C4/code-graph/task-board experience is an upgrade, not a blocker.

## What Chatly Code Adds Beyond OpenHands

Baseline OpenHands-like capabilities are mandatory, but differentiation comes from:

- local desktop UX;
- deterministic code graph before LLM;
- C4 diagrams from graph evidence;
- problem classification;
- task board;
- project knowledge base;
- stricter diff/review/rollback workflow;
- multilingual UI.

## MVP Acceptance Criteria

OpenHands parity MVP requires:

- user can start a conversation/task;
- agent can read files;
- agent can edit files through patch API;
- agent can run approved target-project commands;
- terminal output is visible;
- action/observation events are visible;
- process runtime works;
- git changes are visible;
- file diff is visible;
- user can accept or rollback;
- runtime status is visible;
- settings for LLM provider exist.

Docker runtime can be MVP+1 if process runtime is solid, but architecture must include docker and remote from the start.
