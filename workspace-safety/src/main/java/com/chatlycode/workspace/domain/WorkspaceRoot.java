package com.chatlycode.workspace.domain;

import java.nio.file.Files;
import java.nio.file.Path;

public record WorkspaceRoot(Path path) {

    public WorkspaceRoot {
        if (path == null) {
            throw new IllegalArgumentException("Workspace root is required");
        }
        path = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Workspace root must be an existing directory: " + path);
        }
    }

    public Path resolveInside(String relativePath) {
        Path resolved = path.resolve(relativePath).normalize();
        if (!resolved.startsWith(path)) {
            throw new SecurityException("Path escapes workspace root: " + relativePath);
        }
        return resolved;
    }
}
