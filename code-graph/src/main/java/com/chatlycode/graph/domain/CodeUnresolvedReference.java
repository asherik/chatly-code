package com.chatlycode.graph.domain;

import java.nio.file.Path;
import java.util.List;

public record CodeUnresolvedReference(
        String id,
        String fromNodeId,
        String referenceName,
        String referenceKind,
        Path filePath,
        String language,
        int line,
        List<String> candidates
) {

    public CodeUnresolvedReference {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Reference id must not be blank");
        }
        if (fromNodeId == null || fromNodeId.isBlank()) {
            throw new IllegalArgumentException("From node id must not be blank");
        }
        referenceName = referenceName == null ? "" : referenceName;
        referenceKind = referenceKind == null ? "" : referenceKind;
        candidates = List.copyOf(candidates == null ? List.of() : candidates);
    }
}
