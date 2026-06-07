package com.chatlycode.workspace.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceRootTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesPathsInsideWorkspace() {
        WorkspaceRoot root = new WorkspaceRoot(tempDir);

        Path resolved = root.resolveInside("src/Main.java");

        assertTrue(resolved.startsWith(tempDir.toAbsolutePath().normalize()));
    }

    @Test
    void rejectsPathTraversalOutsideWorkspace() {
        WorkspaceRoot root = new WorkspaceRoot(tempDir);

        assertThrows(SecurityException.class, () -> root.resolveInside("../outside.txt"));
    }
}
