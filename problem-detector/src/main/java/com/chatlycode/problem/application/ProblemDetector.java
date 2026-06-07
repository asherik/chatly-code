package com.chatlycode.problem.application;

import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.problem.domain.DetectedProblem;
import com.chatlycode.problem.domain.ProblemSeverity;

import java.util.ArrayList;
import java.util.List;

public final class ProblemDetector {

    public List<DetectedProblem> detect(CodeGraph graph) {
        List<DetectedProblem> problems = new ArrayList<>();
        graph.nodes().forEach(node -> {
            if (node.kind().equals("java.class") && node.name().endsWith("Manager")) {
                problems.add(new DetectedProblem(
                        "generic-manager-" + node.id().hashCode(),
                        ProblemSeverity.WARNING,
                        "Generic manager name",
                        "Class name suggests mixed responsibilities. Check whether it can be split by domain responsibility.",
                        node.relativePath(),
                        node.line()
                ));
            }
            if (node.kind().equals("java.class") && node.name().endsWith("Util")) {
                problems.add(new DetectedProblem(
                        "generic-util-" + node.id().hashCode(),
                        ProblemSeverity.INFO,
                        "Utility class",
                        "Utility class should stay small and cohesive. Prefer domain services when behavior belongs to a domain.",
                        node.relativePath(),
                        node.line()
                ));
            }
        });
        return List.copyOf(problems);
    }
}
