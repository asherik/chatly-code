package com.chatlycode.architecture.domain;

import java.util.List;

public record ArchitectureSummary(
        int fileCount,
        int nodeCount,
        int edgeCount,
        List<String> topPackages,
        List<ArchitectureContainer> containers,
        List<ArchitectureRelationship> relationships,
        String structurizrDsl,
        String structurizrJson
) {

    public ArchitectureSummary {
        topPackages = List.copyOf(topPackages == null ? List.of() : topPackages);
        containers = List.copyOf(containers == null ? List.of() : containers);
        relationships = List.copyOf(relationships == null ? List.of() : relationships);
        structurizrDsl = structurizrDsl == null ? "" : structurizrDsl;
        structurizrJson = structurizrJson == null ? "" : structurizrJson;
    }

    public String mermaidC4Draft() {
        return structurizrDsl;
    }
}
