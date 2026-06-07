package com.chatlycode.graph.domain;

import com.chatlycode.project.domain.ProjectId;

import java.time.Instant;
import java.util.List;

public record CodeGraph(ProjectId projectId, List<CodeFile> files, List<CodeNode> nodes, List<CodeEdge> edges, Instant indexedAt) {

    public CodeGraph {
        files = List.copyOf(files == null ? List.of() : files);
        nodes = List.copyOf(nodes == null ? List.of() : nodes);
        edges = List.copyOf(edges == null ? List.of() : edges);
        if (indexedAt == null) {
            throw new IllegalArgumentException("Indexed timestamp is required");
        }
    }
}
