package com.chatlycode.agent.tool;

import com.chatlycode.agent.domain.AgentActionType;
import com.chatlycode.workspace.application.WorkspaceSafetyService;
import com.chatlycode.workspace.domain.WorkspaceRoot;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ListFilesTool implements AgentTool {

    private static final int MAX_FILES = 200;

    private final WorkspaceSafetyService workspaceSafetyService;

    public ListFilesTool(WorkspaceSafetyService workspaceSafetyService) {
        this.workspaceSafetyService = workspaceSafetyService;
    }

    @Override
    public AgentActionType type() {
        return AgentActionType.LIST_FILES;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, String> arguments) {
        try {
            List<String> files = workspaceSafetyService.listRelativeFiles(new WorkspaceRoot(context.project().root()));
            List<String> limited = files.stream()
                    .filter(path -> !path.contains("/.git/") && !path.contains("/node_modules/") && !path.contains("/build/"))
                    .limit(MAX_FILES)
                    .toList();
            String detail = limited.stream().collect(Collectors.joining("\n"));
            return AgentToolResult.success("Listed " + limited.size() + " files", detail);
        } catch (Exception exception) {
            return AgentToolResult.failed("Failed to list files", exception.getMessage());
        }
    }
}
