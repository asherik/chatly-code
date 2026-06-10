package com.chatlycode.desktop.graph;

import com.chatlycode.graph.domain.NodeKind;

import java.nio.file.Path;

public record GraphVertex(
        String id,
        String label,
        NodeKind kind,
        String qualifiedName,
        Path filePath,
        String language,
        int line,
        String signature,
        int problemCount
) {

    public boolean hasProblems() {
        return problemCount > 0;
    }
}
