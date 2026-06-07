# MVP Scope Checklist

## Purpose

This file defines the non-negotiable first product scope. If these items are not implemented, the MVP is not ready.

The goal is not to build a full IDE. The goal is to build a local engineering tool that understands a project, builds a code graph, shows architecture/problems/tasks, and lets an agent safely improve the code.

## 1. Project Opening And Scan

The app must open a local project folder and detect:

- primary languages:
  - Java;
  - Rust;
  - TypeScript / JavaScript;
  - Python and Go can follow, but architecture must not block them;
- build tools and package managers:
  - Gradle;
  - Maven;
  - Cargo;
  - npm;
  - pnpm/yarn later;
- Git status;
- test command;
- build command where available;
- main source folders;
- test source folders;
- dependency/build manifest files;
- ignored/generated files.

Important:

- Gradle is mandatory only for building Chatly Code itself.
- User projects can use any build system.
- The app must run detected/approved target-project commands, not hardcoded Gradle commands.

## 2. Code Graph

Code graph is the core of the product.

Minimum graph node types:

- File;
- Module / package;
- Class;
- Interface;
- Record;
- Method;
- Function;
- Field / variable where useful;
- Import;
- Reference.

Minimum graph edge types:

- contains;
- imports;
- calls;
- references;
- extends;
- implements;
- instantiates;
- depends_on;
- returns / type_of where useful.

For Java/Spring, the graph must classify:

- Controller;
- Service;
- Repository;
- Entity;
- DTO;
- Config;
- KafkaListener;
- Scheduled job;
- Feign client;
- RestClient / WebClient client;
- external client;
- route/endpoint.

The graph must be deterministic and stored locally in SQLite. LLM output must not be the source of truth for graph nodes or edges.

## 3. Architecture Explorer

Architecture Explorer is navigation, not decoration.

It must show:

- Controller -> Service -> Repository flow;
- Module -> Module dependencies;
- Class -> Dependencies;
- Incoming dependencies;
- Outgoing dependencies;
- impact radius for selected node;
- files connected to a selected node;
- risks connected to a selected node;
- tasks connected to a selected node.

Clicking a node must show:

- source files;
- direct dependencies;
- incoming dependencies;
- outgoing dependencies;
- detected problems;
- related tasks;
- links to code locations.

## 4. C4 Auto Draft

Minimum C4 output:

- Context;
- Containers;
- Components.

MVP can generate Mermaid Markdown first. A polished diagram UI can come later.

Rules:

- C4 must be based on code graph evidence;
- uncertain relationships must be marked as inferred;
- LLM can improve wording, but cannot invent structure;
- generated C4 must link back to files/nodes where possible.

## 5. Problems

First problem detection rules:

- Controller directly uses Repository;
- Entity leaks into API/UI;
- God Service;
- Huge Class;
- Huge Method;
- Circular Dependency;
- No Tests;
- Dead Code candidate;
- Too many dependencies;
- layer violation;
- high blast-radius file.

Every problem must include:

- type;
- severity;
- confidence;
- evidence;
- affected files/nodes;
- suggested next step.

Evidence is mandatory. No evidence means no problem card.

## 6. Task Generator

The app must generate a task directly from a problem.

Example:

```text
Problem:
OrderController uses OrderRepository directly.

Task:
Move data access from OrderController to OrderService.

Files:
- OrderController
- OrderService
- OrderRepository

Risk:
Medium
```

Generated task must include:

- title;
- problem source;
- goal;
- affected files;
- risk;
- suggested plan;
- definition of done;
- detected test/build command;
- links to graph evidence.

## 7. Graph-Aware Chat

Chat must not be only "chat with files". It must be graph-aware.

Required questions:

```text
Where is an order created?
What can break if UserService changes?
Show flow from controller to database.
Which files depend on this class?
Which tests cover this component?
Why is this file risky?
```

The answer must use:

- graph queries;
- relevant files;
- source snippets;
- line links where available;
- uncertainty markers when graph evidence is incomplete.

## 8. Agent MVP

Basic agent flow:

```text
Plan
Approve
Edit
Diff
Run detected target-project tests/build
Accept / Rollback
```

Rules:

- tooling must use Spring AI Agent Utils first;
- custom code is allowed only for safety, events, policy, graph context, diff and UX wrappers;
- no hidden edits;
- no edit without plan;
- no edit without checkpoint;
- no command without policy;
- every action creates an observation;
- failed commands are shown and used as context for the next step.

## 9. Workspace Safety

Trust requirements:

- Git status;
- checkpoint or branch before edits;
- no hidden edits;
- diff always visible;
- rollback;
- command approval;
- secret redaction;
- workspace root enforcement;
- ignored/secret files protected;
- terminal output visible.

No workspace safety means no agentic editing.

## 10. Minimal UI

This is not an IDE.

Minimum screens/tabs:

- Project Overview;
- Architecture;
- Problems;
- Tasks;
- Chat / Agent Log;
- Diff;
- Terminal / Test Output.

Nice later:

- richer C4 UI;
- file editor;
- PR integration;
- marketplace;
- cloud sync;
- team dashboards.

MVP must stay focused on project understanding and safe agentic change.

