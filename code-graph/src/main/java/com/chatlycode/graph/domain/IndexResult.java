package com.chatlycode.graph.domain;

import java.time.Duration;
import java.util.List;

public record IndexResult(
        boolean success,
        int filesIndexed,
        int filesSkipped,
        int filesErrored,
        int nodesCreated,
        int edgesCreated,
        List<ExtractionError> errors,
        Duration duration
) {

    public IndexResult {
        errors = List.copyOf(errors == null ? List.of() : errors);
        duration = duration == null ? Duration.ZERO : duration;
    }
}
