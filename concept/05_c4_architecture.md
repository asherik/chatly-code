# C4 Architecture

Этот документ описывает C4-архитектуру самого Chatly Code.

## C1: System Context

```mermaid
flowchart LR
    dev["Developer"]
    app["Chatly Code Desktop App"]
    project["Local Source Code Project"]
    llm["LLM Providers\nOpenAI / Anthropic-compatible / Ollama"]
    mcp["MCP Servers"]
    docker["Docker / Podman"]
    git["Git Providers\nGitHub / GitLab / Bitbucket"]

    dev -->|"opens project, reviews plans, accepts diffs"| app
    app -->|"reads, scans, edits with approval"| project
    app -->|"chat/completion/embedding requests"| llm
    app -->|"tool calls"| mcp
    app -->|"sandboxed execution"| docker
    app -->|"optional clone, PR, issue, CI metadata"| git
```

## C2: Containers

```mermaid
flowchart TB
    subgraph desktop["Chatly Code Desktop"]
        javafx["JavaFX UI"]
        backend["Spring Boot Local Backend"]
        sqlite["SQLite Local DB"]
        updater["Updater"]
    end

    subgraph runtime["Execution Runtime"]
        localrt["Local Process Runtime"]
        dockerrt["Docker Sandbox Runtime"]
    end

    subgraph external["External Systems"]
        llm["LLM Providers"]
        mcp["MCP Servers"]
        git["Git Providers"]
    end

    javafx -->|"service calls/events"| backend
    backend --> sqlite
    backend --> localrt
    backend --> dockerrt
    backend --> llm
    backend --> mcp
    backend --> git
    updater --> backend
```

## C3: Spring Boot Backend Components

```mermaid
flowchart TB
    ui["JavaFX UI"]

    subgraph core["Spring Boot Core"]
        projectScanner["Project Scanner"]
        codeGraph["Code Graph Engine"]
        architectureEngine["Architecture Engine"]
        problemDetector["Problem Detector"]
        taskManager["Task Manager"]
        agentCore["Agent Core"]
        toolRuntime["Tool Runtime"]
        workspaceSafety["Workspace Safety"]
        llmGateway["LLM Gateway"]
        knowledgeStore["Knowledge Store"]
        updateService["Update Service"]
    end

    db["SQLite"]
    project["Local Project"]
    docker["Docker Runtime"]
    llm["LLM Provider"]
    mcp["MCP"]

    ui --> taskManager
    ui --> agentCore
    ui --> architectureEngine

    projectScanner --> project
    projectScanner --> knowledgeStore
    codeGraph --> projectScanner
    codeGraph --> project
    codeGraph --> knowledgeStore
    architectureEngine --> codeGraph
    problemDetector --> architectureEngine
    problemDetector --> codeGraph
    problemDetector --> taskManager
    taskManager --> agentCore
    agentCore --> toolRuntime
    agentCore --> codeGraph
    agentCore --> llmGateway
    toolRuntime --> workspaceSafety
    toolRuntime --> project
    toolRuntime --> docker
    toolRuntime --> mcp
    llmGateway --> llm
    knowledgeStore --> db
    updateService --> db
```

## C4: Agent Task Code-Level Flow

```mermaid
sequenceDiagram
    participant User
    participant UI as JavaFX UI
    participant TaskManager
    participant AgentCore
    participant LLMGateway
    participant Tools as Tool Runtime
    participant Safety as Workspace Safety
    participant Project
    participant Tests as Test Runner

    User->>UI: Select task and click Run Agent
    UI->>TaskManager: load task
    TaskManager->>AgentCore: create planning session
    AgentCore->>LLMGateway: request plan
    LLMGateway-->>AgentCore: plan
    AgentCore-->>UI: PlanCreated event
    User->>UI: Approve plan
    UI->>Safety: create branch/checkpoint
    Safety-->>UI: checkpoint ready
    AgentCore->>Tools: read/search/edit files
    Tools->>Project: apply controlled patch
    Tools-->>AgentCore: observations
    AgentCore->>Tests: run build/tests
    Tests-->>AgentCore: test results
    AgentCore-->>UI: NeedsReview with diff
    User->>UI: Accept / rollback / request revision
```

## C4 generation for user projects

Для открытого пользовательского проекта Chatly Code должен генерировать:

- C1 System Context.
- C2 Containers.
- C3 Components.
- C4 Code-level relationships.

Файлы:

```text
docs/architecture/c4-context.md
docs/architecture/c4-containers.md
docs/architecture/c4-components.md
docs/architecture/c4-code.md
```

## Architecture Node Model

Каждый узел C4 должен иметь:

- id;
- name;
- type: system/container/component/code;
- technology;
- description;
- source files;
- incoming dependencies;
- outgoing dependencies;
- detected problems;
- linked tasks.

## C4 UX

C4 не должен быть просто картинкой. Это navigation surface:

```text
Diagram node -> files -> dependencies -> problems -> tasks -> agent fix
```

## Code Graph Backbone

C4-модель строится поверх локального code graph:

```text
CodeNode(file/class/method/route/component)
-> CodeEdge(contains/imports/calls/references/implements/extends)
-> ArchitectureNode(system/container/component/code)
-> C4 diagram
```

Правило: C4 должен опираться на deterministic evidence из AST/resolver, а LLM используется для описаний и группировки только после того, как есть фактический граф.
