package com.chatlycode.agent.tool;

import com.chatlycode.agent.domain.AgentActionType;
import com.chatlycode.workspace.domain.WorkspaceRoot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GrepTool implements AgentTool {

    private static final int MAX_MATCHES = 50;

    @Override
    public AgentActionType type() {
        return AgentActionType.GREP;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, String> arguments) {
        String query = arguments.get("query");
        if (query == null || query.isBlank()) {
            return AgentToolResult.failed("Missing query", "query argument is required");
        }
        WorkspaceRoot root = new WorkspaceRoot(context.project().root());
        List<String> matches = new ArrayList<>();
        try (var stream = Files.walk(root.path(), 8)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> isTextFile(path.getFileName().toString()))
                    .forEach(path -> collectMatches(root, path, query, matches));
        } catch (IOException exception) {
            return AgentToolResult.failed("Grep failed", exception.getMessage());
        }
        if (matches.isEmpty()) {
            return AgentToolResult.success("No matches for '" + query + "'", "");
        }
        return AgentToolResult.success("Found " + matches.size() + " matches", String.join("\n", matches));
    }

    private void collectMatches(WorkspaceRoot root, Path path, String query, List<String> matches) {
        if (matches.size() >= MAX_MATCHES) {
            return;
        }
        try {
            String relative = root.path().relativize(path).toString().replace('\\', '/');
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int index = 0; index < lines.size(); index++) {
                if (lines.get(index).contains(query)) {
                    matches.add(relative + ":" + (index + 1) + ":" + lines.get(index).trim());
                    if (matches.size() >= MAX_MATCHES) {
                        return;
                    }
                }
            }
        } catch (IOException ignored) {
            // skip unreadable files
        }
    }

    private boolean isTextFile(String fileName) {
        return fileName.endsWith(".java") || fileName.endsWith(".kt") || fileName.endsWith(".xml")
                || fileName.endsWith(".properties") || fileName.endsWith(".gradle") || fileName.endsWith(".kts")
                || fileName.endsWith(".md") || fileName.endsWith(".txt") || fileName.endsWith(".json");
    }
}
