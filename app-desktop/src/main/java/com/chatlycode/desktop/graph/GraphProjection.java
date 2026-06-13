package com.chatlycode.desktop.graph;

import java.util.List;

public record GraphProjection(
        List<GraphVertex> vertices,
        List<GraphLink> links,
        boolean truncated,
        int availableNodes
) {

    public GraphProjection {
        vertices = List.copyOf(vertices == null ? List.of() : vertices);
        links = List.copyOf(links == null ? List.of() : links);
    }

    public static GraphProjection empty() {
        return new GraphProjection(List.of(), List.of(), false, 0);
    }
}
