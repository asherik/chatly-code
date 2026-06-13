package com.chatlycode.problem.domain;

import java.nio.file.Path;
import java.util.List;

public record DetectedProblem(
        String id,
        ProblemType type,
        ProblemSeverity severity,
        double confidence,
        String title,
        String description,
        List<String> evidence,
        Path primaryPath,
        int line
) {

    public DetectedProblem {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Problem id must not be blank");
        }
        type = type == null ? ProblemType.GENERIC_NAME : type;
        severity = severity == null ? ProblemSeverity.INFO : severity;
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0 and 1");
        }
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
    }
}
