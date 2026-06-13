package com.chatlycode.agent.tool;

import com.chatlycode.agent.domain.AgentActionType;
import com.chatlycode.git.application.GitService;
import com.chatlycode.workspace.domain.WorkspaceRoot;

import java.util.Map;

public final class GitStatusTool implements AgentTool {

    private final GitService gitService;

    public GitStatusTool(GitService gitService) {
        this.gitService = gitService;
    }

    @Override
    public AgentActionType type() {
        return AgentActionType.GIT_STATUS;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, String> arguments) {
        var status = gitService.status(new WorkspaceRoot(context.project().root()));
        String detail = "branch=" + status.branch() + "\n" + status.porcelainStatus();
        return AgentToolResult.success("Git status on branch " + status.branch(), detail);
    }
}
