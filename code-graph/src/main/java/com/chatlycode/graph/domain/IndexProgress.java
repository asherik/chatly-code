package com.chatlycode.graph.domain;

public record IndexProgress(
        IndexPhase phase,
        int current,
        int total,
        String currentFile
) {
}
