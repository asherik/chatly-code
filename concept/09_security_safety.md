# Security and Safety

## Core Principle

Trust is more important than speed.

The product must make agent behavior visible, reversible and policy-controlled.

## Safety Defaults

- No hidden file changes.
- No silent destructive commands.
- No automatic secret upload.
- No direct modification without checkpoint.
- No untracked tool action.
- No dangerous MCP tool without policy.

## Workspace Safety

Before editing:

```text
1. Check git status.
2. Warn if worktree is dirty.
3. Create branch or checkpoint.
4. Store baseline diff.
5. Start agent execution.
```

If project has no git:

```text
1. Offer to initialize git.
2. Or create internal backup snapshot.
3. Warn that rollback is weaker without git.
```

## Command Risk Levels

Low:

- `git status`;
- `git diff`;
- `ls`;
- `rg`;
- build/test commands already detected.

Medium:

- package manager commands;
- scripts from repository;
- Docker start;
- network calls.

High:

- recursive delete;
- force git commands;
- credential access;
- chmod/chown recursive;
- unknown shell pipelines;
- commands outside workspace.

## Approval UX

Approval dialog should show:

```text
Command:
detected project test command

Why:
Run project tests after code changes.

Risk:
Low

[Allow once] [Always allow for this project] [Deny]
```

For high risk:

```text
High-risk command blocked by default.
User must explicitly confirm.
```

## Secrets Policy

Never send by default:

- `.env`;
- private keys;
- tokens;
- credentials;
- cloud config with secrets;
- local database dumps.

Use redaction:

```text
OPENAI_API_KEY=sk-... -> OPENAI_API_KEY=[REDACTED]
password=... -> password=[REDACTED]
```

## LLM Context Policy

Before sending context:

- apply ignore rules;
- apply secret redaction;
- limit file size;
- prefer relevant snippets;
- log what files/snippets were used.

## MCP Policy

MCP tools must have:

- name;
- source;
- permissions;
- risk level;
- enabled/disabled state;
- user approval requirements.

MCP calls must be logged.

## Sandbox

MVP:

- local process runtime;
- git checkpoint;
- command approval.

Production:

- Docker/Podman sandbox;
- mounted workspace;
- resource limits;
- network policy;
- environment filtering.

## Rollback

Rollback must support:

- rollback whole task;
- rollback single file later;
- discard unaccepted changes;
- restore checkpoint.

UI must show rollback clearly after every agent task.

## Audit Log

Store:

- task;
- plan;
- approvals;
- commands;
- file changes;
- test runs;
- accepted/rejected decisions.

Enterprise edition can expose exportable audit logs.
