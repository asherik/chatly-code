package com.chatlycode.language.spi;

import java.nio.file.Path;
import java.util.List;

public record ExtractedNode(
        String stableId,
        String kind,
        String name,
        String qualifiedName,
        Path relativePath,
        int startLine,
        int endLine,
        String signature,
        List<String> decorators
) {

    public ExtractedNode {
        if (stableId == null || stableId.isBlank()) {
            throw new IllegalArgumentException("Node id must not be blank");
        }
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("Node kind must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Node name must not be blank");
        }
        if (qualifiedName == null || qualifiedName.isBlank()) {
            qualifiedName = name;
        }
        if (relativePath == null) {
            throw new IllegalArgumentException("Node path is required");
        }
        if (startLine < 1) {
            startLine = 1;
        }
        if (endLine < startLine) {
            endLine = startLine;
        }
        signature = signature == null ? "" : signature;
        decorators = List.copyOf(decorators == null ? List.of() : decorators);
    }

    public int line() {
        return startLine;
    }
}
