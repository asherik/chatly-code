package com.chatlycode.agent.tool;

import com.chatlycode.agent.domain.AgentActionType;
import com.chatlycode.workspace.application.WorkspaceSafetyService;
import com.chatlycode.workspace.domain.TextPatch;
import com.chatlycode.workspace.domain.WorkspaceRoot;

import java.util.Map;

public final class ApplyPatchTool implements AgentTool {

    private final WorkspaceSafetyService workspaceSafetyService;

    public ApplyPatchTool(WorkspaceSafetyService workspaceSafetyService) {
        this.workspaceSafetyService = workspaceSafetyService;
    }

    @Override
    public AgentActionType type() {
        return AgentActionType.APPLY_PATCH;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, String> arguments) {
        String path = arguments.get("path");
        String expected = arguments.get("expected");
        String replacement = arguments.get("replacement");
        if (path == null || expected == null || replacement == null) {
            return AgentToolResult.failed("Invalid patch arguments", "path, expected and replacement are required");
        }
        WorkspaceRoot root = new WorkspaceRoot(context.project().root());
        try {
            var preview = workspaceSafetyService.previewPatch(root, new TextPatch(path, expected, replacement));
            if (!preview.hasChanges()) {
                return AgentToolResult.failed("Patch did not match", "Expected text was not found in " + path);
            }
            workspaceSafetyService.applyPatch(root, new TextPatch(path, expected, replacement));
            return AgentToolResult.success("Patch applied to " + path, preview.unifiedDiff());
        } catch (RuntimeException exception) {
            return AgentToolResult.failed("Patch failed for " + path, exception.getMessage());
        }
    }
}
