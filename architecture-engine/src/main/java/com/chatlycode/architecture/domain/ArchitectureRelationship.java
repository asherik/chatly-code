package com.chatlycode.architecture.domain;

public record ArchitectureRelationship(
        String sourceId,
        String targetId,
        String description,
        String technology
) {

    public ArchitectureRelationship {
        sourceId = sourceId == null ? "" : sourceId;
        targetId = targetId == null ? "" : targetId;
        description = description == null ? "" : description;
        technology = technology == null ? "" : technology;
    }
}
