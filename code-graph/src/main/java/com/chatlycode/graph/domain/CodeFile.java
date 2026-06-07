package com.chatlycode.graph.domain;

import java.nio.file.Path;
import java.time.Instant;

public record CodeFile(
        Path relativePath,
        String languageId,
        String contentHash,
        long size,
        Instant modifiedAt,
        Instant indexedAt,
        int nodeCount,
        boolean generated,
        boolean testFile
) {

    public CodeFile {
        if (relativePath == null) {
            throw new IllegalArgumentException("Relative path is required");
        }
        if (languageId == null || languageId.isBlank()) {
            throw new IllegalArgumentException("Language id must not be blank");
        }
        if (contentHash == null || contentHash.isBlank()) {
            throw new IllegalArgumentException("Content hash must not be blank");
        }
        if (modifiedAt == null || indexedAt == null) {
            throw new IllegalArgumentException("Timestamps are required");
        }
    }
}
