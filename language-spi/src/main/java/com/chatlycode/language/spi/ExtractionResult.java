package com.chatlycode.language.spi;

import java.time.Duration;
import java.util.List;

public record ExtractionResult(
        List<ExtractedNode> nodes,
        List<ExtractedEdge> edges,
        List<UnresolvedReference> unresolvedReferences,
        List<ExtractionError> errors,
        Duration duration
) {

    public ExtractionResult(List<ExtractedNode> nodes, List<ExtractedEdge> edges) {
        this(nodes, edges, List.of(), List.of(), Duration.ZERO);
    }

    public ExtractionResult {
        nodes = List.copyOf(nodes == null ? List.of() : nodes);
        edges = List.copyOf(edges == null ? List.of() : edges);
        unresolvedReferences = List.copyOf(unresolvedReferences == null ? List.of() : unresolvedReferences);
        errors = List.copyOf(errors == null ? List.of() : errors);
        duration = duration == null ? Duration.ZERO : duration;
    }
}
