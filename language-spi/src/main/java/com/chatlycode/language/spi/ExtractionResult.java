package com.chatlycode.language.spi;

import java.util.List;

public record ExtractionResult(List<ExtractedNode> nodes, List<ExtractedEdge> edges) {

    public ExtractionResult {
        nodes = List.copyOf(nodes == null ? List.of() : nodes);
        edges = List.copyOf(edges == null ? List.of() : edges);
    }
}
