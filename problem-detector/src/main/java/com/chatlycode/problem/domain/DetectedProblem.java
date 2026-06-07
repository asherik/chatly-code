package com.chatlycode.problem.domain;

import java.nio.file.Path;

public record DetectedProblem(
        String id,
        ProblemSeverity severity,
        String title,
        String description,
        Path relativePath,
        int line
) {
}
