package com.chatlycode.desktop.graph;

import com.chatlycode.problem.domain.ProblemSeverity;

public enum GraphProblemFilter {
    ALL(null),
    ERROR(ProblemSeverity.ERROR),
    WARNING(ProblemSeverity.WARNING),
    INFO(ProblemSeverity.INFO);

    private final ProblemSeverity severity;

    GraphProblemFilter(ProblemSeverity severity) {
        this.severity = severity;
    }

    public ProblemSeverity severity() {
        return severity;
    }
}
