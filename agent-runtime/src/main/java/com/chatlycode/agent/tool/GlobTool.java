package com.chatlycode.agent.tool;

import com.chatlycode.agent.domain.AgentActionType;
import com.chatlycode.workspace.domain.WorkspaceRoot;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GlobTool implements AgentTool {

    private static final int MAX_RESULTS = 100;

    @Override
    public AgentActionType type() {
        return AgentActionType.GLOB;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, String> arguments) {
        String pattern = arguments.getOrDefault("pattern", "**/*");
        WorkspaceRoot root = new WorkspaceRoot(context.project().root());
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        List<String> results = new ArrayList<>();
        try (var stream = Files.walk(root.path(), 10)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        if (results.size() >= MAX_RESULTS) {
                            return;
                        }
                        Path relative = root.path().relativize(path);
                        if (matcher.matches(relative) || matcher.matches(relative.getFileName())) {
                            results.add(relative.toString().replace('\\', '/'));
                        }
                    });
        } catch (IOException exception) {
            return AgentToolResult.failed("Glob failed", exception.getMessage());
        }
        return AgentToolResult.success("Glob matched " + results.size() + " files", String.join("\n", results));
    }
}
