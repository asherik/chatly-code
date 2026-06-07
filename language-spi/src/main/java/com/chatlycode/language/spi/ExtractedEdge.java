package com.chatlycode.language.spi;

public record ExtractedEdge(
        String sourceId,
        String targetId,
        String kind,
        String provenance,
        double confidence,
        Integer line,
        String metadataJson
) {

    public ExtractedEdge {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("Source id must not be blank");
        }
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("Target id must not be blank");
        }
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("Edge kind must not be blank");
        }
        provenance = provenance == null ? "heuristic" : provenance;
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
}
