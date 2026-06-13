package com.chatlycode.desktop.graph;

import com.chatlycode.graph.domain.EdgeKind;

public record GraphLink(
        String sourceId,
        String targetId,
        EdgeKind kind,
        double confidence
) {
}
