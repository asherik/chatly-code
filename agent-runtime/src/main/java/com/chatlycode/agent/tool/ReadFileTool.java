package com.chatlycode.agent.tool;

import com.chatlycode.agent.domain.AgentActionType;
import com.chatlycode.workspace.application.WorkspaceSafetyService;
import com.chatlycode.workspace.domain.WorkspaceRoot;

import java.util.Map;

public final class ReadFileTool implements AgentTool {

    private static final int MAX_CHARS = 100_000;

    private final WorkspaceSafetyService workspaceSafetyService;

    public ReadFileTool(WorkspaceSafetyService workspaceSafetyService) {
        this.workspaceSafetyService = workspaceSafetyService;
    }

    @Override
    public AgentActionType type() {
        return AgentActionType.READ_FILE;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, String> arguments) {
        String path = arguments.get("path");
        if (path == null || path.isBlank()) {
            return AgentToolResult.failed("Missing path", "path argument is required");
        }
        try {
            String content = workspaceSafetyService.readText(new WorkspaceRoot(context.project().root()), path);
            if (content.length() > MAX_CHARS) {
                content = content.substring(0, MAX_CHARS) + "\n...[truncated]";
            }
            int lines = content.isEmpty() ? 0 : content.split("\\R", -1).length;
            return AgentToolResult.success("Read file " + path + " (" + lines + " lines)", content);
        } catch (RuntimeException exception) {
            return AgentToolResult.failed("Failed to read " + path, exception.getMessage());
        }
    }
}
