package com.chatlycode.graph.domain;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public record CodeNode(
        String id,
        NodeKind kind,
        String name,
        String qualifiedName,
        Path filePath,
        String language,
        int startLine,
        int endLine,
        String signature,
        List<String> decorators,
        Instant updatedAt
) {

    public CodeNode {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Node id must not be blank");
        }
        if (kind == null) {
            throw new IllegalArgumentException("Node kind is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Node name must not be blank");
        }
        if (qualifiedName == null || qualifiedName.isBlank()) {
            qualifiedName = name;
        }
        if (filePath == null) {
            throw new IllegalArgumentException("File path is required");
        }
        language = language == null ? "unknown" : language;
        signature = signature == null ? "" : signature;
        decorators = List.copyOf(decorators == null ? List.of() : decorators);
        if (endLine < startLine) {
            endLine = startLine;
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("Updated timestamp is required");
        }
    }

    public Path relativePath() {
        return filePath;
    }

    public int line() {
        return startLine;
    }

    public boolean hasDecorator(String decorator) {
        String normalized = decorator.toLowerCase(Locale.ROOT);
        return decorators.stream().anyMatch(value -> value.toLowerCase(Locale.ROOT).equals(normalized));
    }

    public boolean isController() {
        return hasDecorator("Controller") || hasDecorator("RestController") || name.endsWith("Controller");
    }

    public boolean isService() {
        return hasDecorator("Service") || name.endsWith("Service");
    }

    public boolean isRepository() {
        return hasDecorator("Repository") || name.endsWith("Repository");
    }

    public boolean isEntity() {
        return hasDecorator("Entity");
    }
}
