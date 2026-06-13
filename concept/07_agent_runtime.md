# Agent Runtime

## Core Loop

Agent runtime is an event-driven loop:

```text
Task
-> Context building
-> Plan
-> Approval
-> Action
-> Observation
-> Next action
-> Tests
-> Review
-> Done / Failed / Rollback
```

This loop is mandatory OpenHands-like behavior. The agent must be able to inspect files, modify files through controlled patches, run approved commands, observe results and continue until the task reaches review, failure or rollback.

## Runtime Boundary

Agent runtime must be separated from UI and app orchestration.

Required runtime modes:

```text
process
docker
remote
```

MVP may implement only process runtime first, but interfaces must support Docker/Podman and remote runtime without redesign.

Runtime responsibilities:

- execute approved commands;
- stream stdout/stderr to Terminal;
- enforce command policy;
- apply timeouts;
- report runtime status;
- emit command observations.

Sandbox responsibilities:

- start runtime;
- stop runtime;
- pause/resume where supported;
- filter environment variables;
- mount workspace safely;
- apply resource limits where supported.

## Spring AI Agent Utils Requirement

Coding-agent tools must be based on `spring-ai-agent-utils` where possible.

Reference local example:

```text
C:\projects\chatly-code\examples\spring-ai-agent-utils-main
```

Use it for:

- file system tools;
- shell tools;
- grep/glob tools;
- todo/task tools;
- skills;
- subagent framework;
- memory tools;
- A2A subagents.

Chatly Code must wrap these tools with:

- workspace root restrictions;
- approval policy;
- sandbox/runtime policy;
- secret redaction;
- action/observation event emission;
- diff collection;
- rollback support.

Do not write a custom coding-agent tool framework unless the library is proven insufficient for a specific requirement.

## Task States

```text
CREATED
ANALYZING
PLANNING
WAITING_APPROVAL
CHECKPOINTING
EXECUTING
RUNNING_TESTS
NEEDS_REVIEW
DONE
FAILED
ROLLED_BACK
CANCELLED
```

## Event Types

```text
UserMessage
AgentMessage
AgentThought
PlanCreated
PlanApproved
ToolActionStarted
ToolActionFinished
ToolObservation
FileRead
FileChanged
CommandStarted
CommandFinished
TestRunStarted
TestRunFinished
RiskWarning
ApprovalRequested
TaskStatusChanged
TaskFinished
TaskFailed
RollbackFinished
```

## Tool Types

### ReadFileTool

Use/adapt Spring AI Agent Utils file system tooling. Reads file content with size limits.

### SearchTool

Use/adapt Spring AI Agent Utils grep/search tooling where possible.

### ListFilesTool

Use/adapt Spring AI Agent Utils glob/file tooling where possible.

### ApplyPatchTool

Use/adapt Spring AI Agent Utils file edit tooling, but all writes must pass through Chatly Code `workspace-safety`. Must emit diff.

### ShellTool

Use/adapt Spring AI Agent Utils shell tooling. Dangerous commands require approval and runtime policy.

### GitTool

Supports:

- status;
- branch;
- diff;
- commit;
- restore through safe APIs only;
- log.

### BuildTool

Runs detected build command.

### TestTool

Runs detected test command.

### DockerTool

Creates and controls Docker/Podman sandbox runtime.

### RuntimeTool

Reports runtime status and controls process/docker/remote runtime lifecycle.

### ConversationTool

Reads and writes durable conversation/task trajectory events.

### McpTool

Calls configured MCP servers under policy control.

### C4DiagramTool

Generates or updates C4 diagrams.

## Approval Rules

No approval required:

- read file;
- list files;
- search;
- git status;
- git diff;

Approval required:

- edit file;
- run shell command;
- create branch;
- install dependency;
- network call through tool;
- Docker container start;

High-risk approval required:

- delete files;
- recursive changes;
- run scripts from project;
- package manager install;
- commands touching credentials;
- force push;
- destructive git commands.

## Planning Rules

Before editing, agent must produce:

- task summary;
- files likely to change;
- step-by-step plan;
- test strategy;
- risk level;
- rollback strategy.

## Context Building

Agent context should include:

- task;
- project rules;
- graph context from code-graph-engine;
- relevant code nodes and edges;
- relevant files;
- architecture nodes;
- related problems;
- previous task history;
- build/test commands;
- git status;
- constraints.

Before reading files manually, the agent should query the local code graph:

```text
task text
-> graph search/explore
-> relevant symbols, flows, callers/callees, source snippets
-> plan
```

Manual file reads are allowed, but graph context is the first-class discovery mechanism.

## Failure Handling

If tests fail:

- show failing command;
- show failing tests;
- summarize likely cause;
- offer:
  - fix;
  - rollback;
  - ask user;
  - mark failed.

If tool command fails:

- do not hide failure;
- add event;
- decide next step;
- avoid repeating same failing command endlessly.

## Agent Roles

MVP can use one agent with modes:

- Analyzer.
- Planner.
- Implementer.
- Tester.
- Reviewer.

Later multi-agent:

- ArchitectAgent;
- FixerAgent;
- ReviewerAgent;
- TestAgent;
- SecurityAgent.

## OpenHands Ideas to Preserve

- actions and observations are first-class;
- every conversation has durable history;
- tools are explicit;
- agent can plan and track tasks;
- sandbox is separate from UI;
- skills/rules extend behavior.
- agent can read files, edit files and run commands;
- process, docker and remote runtimes are first-class;
- terminal output is visible;
- git changes and diff are first-class review surfaces;
- direct "give task and agent works" flow must work even without C4/task-board usage.

## CodeGraph Ideas to Preserve

- deterministic AST extraction before LLM reasoning;
- local SQLite graph;
- nodes, edges, files and unresolved references;
- resolver pass after indexing;
- framework-aware routes and synthetic edges only with clear provenance;
- watcher and debounce sync;
- stale-context warning;
- explore-style graph tool that gives enough context to avoid grep/read loops.
