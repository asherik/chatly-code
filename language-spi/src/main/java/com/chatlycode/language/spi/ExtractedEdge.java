package com.chatlycode.language.spi;

public record ExtractedEdge(String fromNodeId, String toName, String kind) {

    public ExtractedEdge {
        if (fromNodeId == null || fromNodeId.isBlank()) {
            throw new IllegalArgumentException("From node id must not be blank");
        }
        if (toName == null || toName.isBlank()) {
            throw new IllegalArgumentException("Target name must not be blank");
        }
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("Edge kind must not be blank");
        }
    }
}
