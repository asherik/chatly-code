package com.chatlycode.problem.application;

import com.chatlycode.graph.domain.CodeEdge;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.domain.EdgeKind;
import com.chatlycode.graph.domain.NodeKind;
import com.chatlycode.problem.domain.DetectedProblem;
import com.chatlycode.problem.domain.ProblemSeverity;
import com.chatlycode.problem.domain.ProblemType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ProblemDetector {

    private static final int HUGE_CLASS_LINE_THRESHOLD = 400;
    private static final int HUGE_METHOD_COUNT_THRESHOLD = 30;
    private static final int TOO_MANY_IMPORTS_THRESHOLD = 25;
    private static final int HIGH_BLAST_RADIUS_THRESHOLD = 12;

    public List<DetectedProblem> detect(CodeGraph graph) {
        Map<Path, List<CodeNode>> nodesByFile = graph.nodes().stream()
                .collect(Collectors.groupingBy(CodeNode::filePath));
        Map<Path, List<CodeEdge>> edgesByFile = graph.edges().stream()
                .collect(Collectors.groupingBy(edge -> findNodePath(graph, edge.sourceId())));

        List<DetectedProblem> problems = new ArrayList<>();
        problems.addAll(detectLayerViolations(graph, nodesByFile, edgesByFile));
        problems.addAll(detectEntityLeaks(nodesByFile, edgesByFile));
        problems.addAll(detectHugeClasses(nodesByFile));
        problems.addAll(detectTooManyDependencies(graph, nodesByFile));
        problems.addAll(detectGenericNames(graph.nodes()));
        problems.addAll(detectHighBlastRadius(graph));
        return List.copyOf(problems);
    }

    private List<DetectedProblem> detectLayerViolations(
            CodeGraph graph,
            Map<Path, List<CodeNode>> nodesByFile,
            Map<Path, List<CodeEdge>> edgesByFile
    ) {
        List<DetectedProblem> problems = new ArrayList<>();
        for (CodeNode node : graph.nodes()) {
            if (!node.isController()) {
                continue;
            }
            List<String> evidence = new ArrayList<>();
            for (CodeEdge edge : edgesByFile.getOrDefault(node.filePath(), List.of())) {
                if (!edge.sourceId().equals(node.id()) && !isEdgeFromSameType(graph, edge, node)) {
                    continue;
                }
                if ((edge.kind() == EdgeKind.TYPE_OF || edge.kind() == EdgeKind.IMPORTS || edge.kind() == EdgeKind.REFERENCES)
                        && edge.targetId().contains("Repository")) {
                    evidence.add(edge.kind().name().toLowerCase() + ": " + edge.targetId());
                }
            }
            if (!evidence.isEmpty()) {
                problems.add(new DetectedProblem(
                        "layer-violation-" + node.id().hashCode(),
                        ProblemType.LAYER_VIOLATION,
                        ProblemSeverity.ERROR,
                        0.92,
                        "Controller uses repository directly",
                        "Move data access from the controller into a service layer.",
                        evidence,
                        node.filePath(),
                        node.startLine()
                ));
            }
        }
        return problems;
    }

    private boolean isEdgeFromSameType(CodeGraph graph, CodeEdge edge, CodeNode typeNode) {
        return graph.nodes().stream()
                .anyMatch(node -> node.id().equals(edge.sourceId()) && node.filePath().equals(typeNode.filePath()));
    }

    private List<DetectedProblem> detectEntityLeaks(
            Map<Path, List<CodeNode>> nodesByFile,
            Map<Path, List<CodeEdge>> edgesByFile
    ) {
        List<DetectedProblem> problems = new ArrayList<>();
        for (Map.Entry<Path, List<CodeNode>> entry : nodesByFile.entrySet()) {
            boolean controller = entry.getValue().stream().anyMatch(CodeNode::isController);
            if (!controller) {
                continue;
            }
            List<String> evidence = edgesByFile.getOrDefault(entry.getKey(), List.of()).stream()
                    .filter(edge -> edge.kind() == EdgeKind.IMPORTS)
                    .filter(edge -> edge.targetId().contains(".entity.") || edge.targetId().endsWith("Entity"))
                    .map(edge -> "Imports entity type: " + edge.targetId())
                    .toList();
            if (!evidence.isEmpty()) {
                CodeNode controllerNode = entry.getValue().stream()
                        .filter(CodeNode::isController)
                        .findFirst()
                        .orElse(entry.getValue().getFirst());
                problems.add(new DetectedProblem(
                        "entity-leak-" + controllerNode.id().hashCode(),
                        ProblemType.ENTITY_LEAK,
                        ProblemSeverity.WARNING,
                        0.85,
                        "Entity leaks into controller/API layer",
                        "Expose DTOs or view models instead of persistence entities in the controller.",
                        evidence,
                        controllerNode.filePath(),
                        controllerNode.startLine()
                ));
            }
        }
        return problems;
    }

    private List<DetectedProblem> detectHugeClasses(Map<Path, List<CodeNode>> nodesByFile) {
        List<DetectedProblem> problems = new ArrayList<>();
        for (Map.Entry<Path, List<CodeNode>> entry : nodesByFile.entrySet()) {
            long methodCount = entry.getValue().stream().filter(node -> node.kind() == NodeKind.METHOD).count();
            int estimatedLines = entry.getValue().stream().mapToInt(CodeNode::endLine).max().orElse(0);
            if (estimatedLines < HUGE_CLASS_LINE_THRESHOLD && methodCount < HUGE_METHOD_COUNT_THRESHOLD) {
                continue;
            }
            CodeNode primary = entry.getValue().stream()
                    .filter(node -> node.kind() == NodeKind.CLASS || node.kind() == NodeKind.INTERFACE || node.kind() == NodeKind.RECORD)
                    .findFirst()
                    .orElse(entry.getValue().getFirst());
            problems.add(new DetectedProblem(
                    "huge-class-" + primary.id().hashCode(),
                    ProblemType.HUGE_CLASS,
                    ProblemSeverity.WARNING,
                    0.75,
                    "Huge class candidate",
                    "Split responsibilities into smaller cohesive types.",
                    List.of(
                            "Estimated last declaration line: " + estimatedLines,
                            "Detected methods: " + methodCount
                    ),
                    entry.getKey(),
                    primary.startLine()
            ));
        }
        return problems;
    }

    private List<DetectedProblem> detectTooManyDependencies(CodeGraph graph, Map<Path, List<CodeNode>> nodesByFile) {
        List<DetectedProblem> problems = new ArrayList<>();
        for (Map.Entry<Path, List<CodeNode>> entry : nodesByFile.entrySet()) {
            long importCount = graph.edges().stream()
                    .filter(edge -> edge.kind() == EdgeKind.IMPORTS)
                    .filter(edge -> findNodePath(graph, edge.sourceId()).equals(entry.getKey()))
                    .count();
            if (importCount < TOO_MANY_IMPORTS_THRESHOLD) {
                continue;
            }
            CodeNode primary = entry.getValue().getFirst();
            problems.add(new DetectedProblem(
                    "too-many-deps-" + primary.id().hashCode(),
                    ProblemType.TOO_MANY_DEPENDENCIES,
                    ProblemSeverity.INFO,
                    0.7,
                    "Too many dependencies",
                    "Review whether the type can be split or dependencies reduced.",
                    List.of("Import edges: " + importCount),
                    entry.getKey(),
                    primary.startLine()
            ));
        }
        return problems;
    }

    private List<DetectedProblem> detectGenericNames(List<CodeNode> nodes) {
        List<DetectedProblem> problems = new ArrayList<>();
        for (CodeNode node : nodes) {
            if (node.kind() != NodeKind.CLASS && node.kind() != NodeKind.INTERFACE) {
                continue;
            }
            if (node.name().endsWith("Manager")) {
                problems.add(genericNameProblem(node, ProblemSeverity.WARNING, "Generic manager name"));
            } else if (node.name().endsWith("Util") || node.name().endsWith("Utils")) {
                problems.add(genericNameProblem(node, ProblemSeverity.INFO, "Utility class name"));
            }
        }
        return problems;
    }

    private DetectedProblem genericNameProblem(CodeNode node, ProblemSeverity severity, String title) {
        return new DetectedProblem(
                "generic-name-" + node.id().hashCode(),
                ProblemType.GENERIC_NAME,
                severity,
                0.6,
                title,
                "Prefer a domain-specific name that explains responsibility.",
                List.of("Type: " + node.qualifiedName()),
                node.filePath(),
                node.startLine()
        );
    }

    private List<DetectedProblem> detectHighBlastRadius(CodeGraph graph) {
        Map<String, Set<String>> incoming = new HashMap<>();
        for (CodeEdge edge : graph.edges()) {
            if (edge.kind() != EdgeKind.IMPORTS && edge.kind() != EdgeKind.REFERENCES && edge.kind() != EdgeKind.CALLS) {
                continue;
            }
            incoming.computeIfAbsent(edge.targetId(), ignored -> new HashSet<>()).add(edge.sourceId());
        }
        List<DetectedProblem> problems = new ArrayList<>();
        for (CodeNode node : graph.nodes()) {
            int inbound = incoming.getOrDefault(node.qualifiedName(), Set.of()).size()
                    + incoming.getOrDefault(node.id(), Set.of()).size();
            if (inbound < HIGH_BLAST_RADIUS_THRESHOLD) {
                continue;
            }
            problems.add(new DetectedProblem(
                    "blast-radius-" + node.id().hashCode(),
                    ProblemType.HIGH_BLAST_RADIUS,
                    ProblemSeverity.WARNING,
                    0.8,
                    "High blast-radius file",
                    "Changes here may affect many dependents. Add tests and review impact radius before editing.",
                    List.of("Inbound references: " + inbound),
                    node.filePath(),
                    node.startLine()
            ));
        }
        return problems;
    }

    private Path findNodePath(CodeGraph graph, String nodeId) {
        return graph.nodes().stream()
                .filter(node -> node.id().equals(nodeId))
                .map(CodeNode::filePath)
                .findFirst()
                .orElse(Path.of(nodeId));
    }
}
