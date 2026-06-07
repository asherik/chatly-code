package com.chatlycode.architecture.domain;

import java.util.List;

public record ArchitectureSummary(int fileCount, int nodeCount, int edgeCount, List<String> topPackages, String mermaidC4Draft) {

    public ArchitectureSummary {
        topPackages = List.copyOf(topPackages == null ? List.of() : topPackages);
    }
}
