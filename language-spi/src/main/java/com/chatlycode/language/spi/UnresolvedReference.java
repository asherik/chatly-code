package com.chatlycode.language.spi;

import java.nio.file.Path;
import java.util.List;

public record UnresolvedReference(
        String fromNodeId,
        String referenceName,
        String referenceKind,
        Path relativePath,
        int line,
        List<String> candidates
) {

    public UnresolvedReference {
        if (fromNodeId == null || fromNodeId.isBlank()) {
            throw new IllegalArgumentException("From node id must not be blank");
        }
        referenceName = referenceName == null ? "" : referenceName;
        referenceKind = referenceKind == null ? "" : referenceKind;
        candidates = List.copyOf(candidates == null ? List.of() : candidates);
    }
}
