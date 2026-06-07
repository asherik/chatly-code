# Domain Model

## Project

Represents a local source code project.

Fields:

- id;
- name;
- rootPath;
- detectedStack;
- gitInfo;
- createdAt;
- lastOpenedAt;
- settings;
- scanStatus.

## ProjectScan

Represents one scan result.

Fields:

- id;
- projectId;
- startedAt;
- finishedAt;
- status;
- detectedLanguages;
- buildTools;
- testCommands;
- dependencyFiles;
- readmeFiles;
- sourceFileCount;
- ignoredFileCount;
- summary.

## ArchitectureNode

Represents C4 node or internal architecture element.

Fields:

- id;
- projectId;
- scanId;
- level: context/container/component/code;
- type;
- name;
- technology;
- description;
- sourceFiles;
- responsibilities.

## CodeFile

Represents indexed source file in the local code graph.

Fields:

- path;
- contentHash;
- language;
- size;
- modifiedAt;
- indexedAt;
- nodeCount;
- generated;
- testFile;
- errorsJson.

## CodeNode

Represents deterministic code symbol extracted from AST or framework metadata.

Fields:

- id;
- kind;
- name;
- qualifiedName;
- filePath;
- language;
- startLine;
- endLine;
- startColumn;
- endColumn;
- docstring;
- signature;
- visibility;
- exported;
- async;
- static;
- abstract;
- decoratorsJson;
- typeParametersJson;
- updatedAt.

## CodeEdge

Represents relationship between code nodes.

Fields:

- id;
- sourceNodeId;
- targetNodeId;
- kind;
- metadataJson;
- line;
- column;
- provenance;
- confidence.

## CodeUnresolvedReference

Represents reference collected during extraction and resolved later.

Fields:

- id;
- fromNodeId;
- referenceName;
- referenceKind;
- filePath;
- language;
- line;
- column;
- candidatesJson.

## ArchitectureEdge

Represents dependency/relation.

Fields:

- id;
- sourceNodeId;
- targetNodeId;
- relationType;
- description;
- evidence;
- confidence.

## Problem

Represents detected issue.

Fields:

- id;
- projectId;
- scanId;
- type: bug/risk/improvement/tech_debt/feature/security;
- severity: low/medium/high/critical;
- confidence;
- title;
- description;
- evidence;
- affectedFiles;
- architectureNodeIds;
- status: open/ignored/converted_to_task/resolved;

## Task

Represents actionable work.

Fields:

- id;
- projectId;
- sourceProblemId;
- type;
- title;
- description;
- acceptanceCriteria;
- priority;
- risk;
- status;
- affectedFiles;
- createdAt;
- updatedAt;

## Conversation

Represents agent session.

Fields:

- id;
- projectId;
- taskId;
- title;
- modelProfileId;
- status;
- createdAt;
- updatedAt;

## AgentEvent

Represents durable event in session.

Fields:

- id;
- conversationId;
- type;
- timestamp;
- actor: user/agent/tool/system;
- payloadJson;
- visible;

## ToolAction

Represents requested tool call.

Fields:

- id;
- conversationId;
- toolName;
- argumentsJson;
- risk;
- approvalState;
- startedAt;
- finishedAt;
- resultStatus;

## FileChange

Represents controlled file modification.

Fields:

- id;
- taskId;
- conversationId;
- path;
- changeType;
- diff;
- createdAt;
- accepted;

## TestRun

Represents test/build result.

Fields:

- id;
- taskId;
- command;
- exitCode;
- stdoutPath;
- stderrPath;
- startedAt;
- finishedAt;
- summary;

## ModelProfile

Fields:

- id;
- name;
- provider;
- model;
- baseUrl;
- apiKeySecretRef;
- useForChat;
- useForPlanning;
- useForEmbeddings;

## Settings

Fields:

- theme;
- language;
- updateChannel;
- telemetryEnabled;
- defaultModelProfileId;
- sandboxMode;
- commandPolicy;
- secretsPolicy.

## SQLite Tables

```text
projects
project_scans
code_files
code_nodes
code_edges
code_unresolved_refs
code_graph_metadata
architecture_nodes
architecture_edges
problems
tasks
conversations
agent_events
tool_actions
file_changes
test_runs
model_profiles
settings
localization_messages
update_state
```

## Domain Rules

- A problem can exist without a task.
- A task can exist without a problem if user created it manually.
- A task can have many conversations.
- A conversation can have many tool actions.
- Every file change must belong to a task or explicit user-approved session.
- Every accepted file change must be reviewable through diff.
- Code graph nodes and edges are deterministic evidence, not LLM guesses.
- Architecture nodes may reference many code nodes.
- Problems should link to code nodes, code edges or files as evidence.
- Unresolved references are valid graph data and must not be silently treated as resolved.
