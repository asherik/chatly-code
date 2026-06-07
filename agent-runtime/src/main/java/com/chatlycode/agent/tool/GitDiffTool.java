package com.chatlycode.agent.tool;

import com.chatlycode.agent.domain.AgentActionType;
import com.chatlycode.git.application.GitService;
import com.chatlycode.workspace.domain.WorkspaceRoot;

import java.util.Map;

public final class GitDiffTool implements AgentTool {

    private final GitService gitService;

    public GitDiffTool(GitService gitService) {
        this.gitService = gitService;
    }

    @Override
    public AgentActionType type() {
        return AgentActionType.GIT_DIFF;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, String> arguments) {
        String diff = gitService.diff(new WorkspaceRoot(context.project().root()));
        if (diff.isBlank()) {
            return AgentToolResult.success("No git diff", "");
        }
        return AgentToolResult.success("Git diff captured", diff);
    }
}
