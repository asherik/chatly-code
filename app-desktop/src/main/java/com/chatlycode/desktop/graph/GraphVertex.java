package com.chatlycode.desktop.graph;

import com.chatlycode.graph.domain.NodeKind;

import java.nio.file.Path;
import java.util.List;

public record GraphVertex(
        String id,
        String label,
        NodeKind kind,
        String qualifiedName,
        Path filePath,
        String language,
        int line,
        String signature,
        int problemCount,
        List<String> problems
) {

    public GraphVertex {
        problems = List.copyOf(problems == null ? List.of() : problems);
    }

    public boolean hasProblems() {
        return problemCount > 0;
    }
}
