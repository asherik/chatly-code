package com.chatlycode.graph.domain;

public record CodeEdge(
        String sourceId,
        String targetId,
        EdgeKind kind,
        EdgeProvenance provenance,
        double confidence,
        Integer line,
        String metadataJson
) {

    public CodeEdge {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("Source id must not be blank");
        }
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("Target id must not be blank");
        }
        if (kind == null) {
            throw new IllegalArgumentException("Edge kind is required");
        }
        provenance = provenance == null ? EdgeProvenance.HEURISTIC : provenance;
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0 and 1");
        }
        metadataJson = metadataJson == null ? "" : metadataJson;
    }

    public String fromNodeId() {
        return sourceId;
    }

    public String toName() {
        return targetId;
    }

    public String kind() {
        return kind.name().toLowerCase();
    }
}
