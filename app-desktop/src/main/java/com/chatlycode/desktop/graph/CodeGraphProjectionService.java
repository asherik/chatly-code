package com.chatlycode.desktop.graph;

import com.chatlycode.graph.domain.CodeEdge;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.domain.EdgeKind;
import com.chatlycode.graph.domain.NodeKind;
import com.chatlycode.problem.domain.DetectedProblem;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class CodeGraphProjectionService {

    private static final EnumSet<NodeKind> ARCHITECTURE_KINDS = EnumSet.of(
            NodeKind.PACKAGE,
            NodeKind.MODULE,
            NodeKind.NAMESPACE,
            NodeKind.CLASS,
            NodeKind.INTERFACE,
            NodeKind.RECORD,
            NodeKind.ENUM,
            NodeKind.COMPONENT,
            NodeKind.ROUTE,
            NodeKind.EXTERNAL_SERVICE
    );
    private static final EnumSet<NodeKind> METHOD_KINDS = EnumSet.of(NodeKind.METHOD, NodeKind.FUNCTION, NodeKind.CONSTRUCTOR);
    private static final EnumSet<NodeKind> IMPORT_KINDS = EnumSet.of(NodeKind.IMPORT, NodeKind.EXPORT);
    private static final EnumSet<EdgeKind> ARCHITECTURE_EDGES = EnumSet.of(
            EdgeKind.CONTAINS,
            EdgeKind.REFERENCES,
            EdgeKind.EXTENDS,
            EdgeKind.IMPLEMENTS,
            EdgeKind.HANDLES_ROUTE,
            EdgeKind.DECORATES
    );
    private static final EnumSet<EdgeKind> DEPENDENCY_EDGES = EnumSet.of(
            EdgeKind.IMPORTS,
            EdgeKind.REFERENCES,
            EdgeKind.CALLS,
            EdgeKind.EXTENDS,
            EdgeKind.IMPLEMENTS,
            EdgeKind.TYPE_OF,
            EdgeKind.INSTANTIATES,
            EdgeKind.RETURNS,
            EdgeKind.TESTS
    );

    public GraphProjection project(CodeGraph graph, List<DetectedProblem> problems, GraphProjectionOptions options) {
        if (graph == null) {
            return GraphProjection.empty();
        }

        GraphProjectionOptions effectiveOptions = options == null ? GraphProjectionOptions.defaults() : options;
        Map<Path, List<DetectedProblem>> problemsByPath = problemsByPath(problems);
        Map<String, CodeNode> nodesById = graph.nodes().stream()
                .collect(Collectors.toMap(CodeNode::id, node -> node, (first, ignored) -> first, LinkedHashMap::new));

        Set<String> selectedIds = selectNodeIds(graph, nodesById, problemsByPath, effectiveOptions);
        int available = selectedIds.size();
        boolean truncated = selectedIds.size() > effectiveOptions.maxNodes();
        if (truncated) {
            selectedIds = selectedIds.stream()
                    .sorted((left, right) -> nodeComparator(problemsByPath).compare(nodesById.get(left), nodesById.get(right)))
                    .limit(effectiveOptions.maxNodes())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        Set<String> finalSelectedIds = selectedIds;
        List<GraphLink> links = graph.edges().stream()
                .filter(edge -> finalSelectedIds.contains(edge.sourceId()) && finalSelectedIds.contains(edge.targetId()))
                .filter(edge -> edgeAllowed(edge, effectiveOptions))
                .map(edge -> new GraphLink(edge.sourceId(), edge.targetId(), edge.kind(), edge.confidence()))
                .limit(effectiveOptions.maxNodes() * 3L)
                .toList();

        List<GraphVertex> vertices = selectedIds.stream()
                .map(nodesById::get)
                .filter(node -> node != null)
                .map(node -> toVertex(node, problemsByPath))
                .toList();

        return new GraphProjection(vertices, links, truncated, available);
    }

    private Set<String> selectNodeIds(
            CodeGraph graph,
            Map<String, CodeNode> nodesById,
            Map<Path, List<DetectedProblem>> problemsByPath,
            GraphProjectionOptions options
    ) {
        if (!options.focusNodeId().isBlank() && nodesById.containsKey(options.focusNodeId())) {
            return focusedIds(graph, options.focusNodeId(), options.mode() == GraphMode.IMPACT ? 2 : 1);
        }

        Set<String> ids = graph.nodes().stream()
                .filter(node -> nodeAllowed(node, options))
                .filter(node -> matchesSearch(node, options.searchText()) || pathHasProblem(node, problemsByPath))
                .sorted(nodeComparator(problemsByPath))
                .map(CodeNode::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!options.searchText().isBlank()) {
            ids.addAll(neighborIds(graph, ids, 1));
        }
        return ids;
    }

    private Set<String> focusedIds(CodeGraph graph, String focusNodeId, int depth) {
        Set<String> ids = new LinkedHashSet<>();
        ids.add(focusNodeId);
        Set<String> frontier = Set.of(focusNodeId);
        for (int step = 0; step < depth; step++) {
            Set<String> next = neighborIds(graph, frontier, 1);
            next.removeAll(ids);
            ids.addAll(next);
            frontier = next;
            if (frontier.isEmpty()) {
                break;
            }
        }
        return ids;
    }

    private Set<String> neighborIds(CodeGraph graph, Set<String> ids, int depth) {
        Set<String> result = new LinkedHashSet<>();
        Set<String> frontier = new LinkedHashSet<>(ids);
        for (int step = 0; step < depth; step++) {
            Set<String> next = new LinkedHashSet<>();
            for (CodeEdge edge : graph.edges()) {
                if (frontier.contains(edge.sourceId())) {
                    next.add(edge.targetId());
                }
                if (frontier.contains(edge.targetId())) {
                    next.add(edge.sourceId());
                }
            }
            result.addAll(next);
            frontier = next;
        }
        return result;
    }

    private boolean nodeAllowed(CodeNode node, GraphProjectionOptions options) {
        if (options.mode() == GraphMode.FILES) {
            return node.kind() == NodeKind.FILE || ARCHITECTURE_KINDS.contains(node.kind());
        }
        if (options.showMethods() && METHOD_KINDS.contains(node.kind())) {
            return true;
        }
        if (options.showImports() && IMPORT_KINDS.contains(node.kind())) {
            return true;
        }
        if (node.kind() == NodeKind.EXTERNAL_SERVICE) {
            return options.showExternal();
        }
        if (options.mode() == GraphMode.DEPENDENCIES) {
            return ARCHITECTURE_KINDS.contains(node.kind()) || node.kind() == NodeKind.FILE;
        }
        return ARCHITECTURE_KINDS.contains(node.kind());
    }

    private boolean edgeAllowed(CodeEdge edge, GraphProjectionOptions options) {
        if (options.mode() == GraphMode.FILES) {
            return edge.kind() == EdgeKind.CONTAINS;
        }
        if (options.mode() == GraphMode.DEPENDENCIES || options.mode() == GraphMode.IMPACT) {
            return DEPENDENCY_EDGES.contains(edge.kind()) || edge.kind() == EdgeKind.CONTAINS;
        }
        return ARCHITECTURE_EDGES.contains(edge.kind());
    }

    private boolean matchesSearch(CodeNode node, String searchText) {
        if (searchText == null || searchText.isBlank()) {
            return true;
        }
        String query = searchText.toLowerCase(Locale.ROOT);
        return node.name().toLowerCase(Locale.ROOT).contains(query)
                || node.qualifiedName().toLowerCase(Locale.ROOT).contains(query)
                || node.filePath().toString().toLowerCase(Locale.ROOT).contains(query);
    }

    private Comparator<CodeNode> nodeComparator(Map<Path, List<DetectedProblem>> problemsByPath) {
        return Comparator
                .comparingInt((CodeNode node) -> -problemCount(node, problemsByPath))
                .thenComparingInt(node -> nodeKindRank(node.kind()))
                .thenComparing(CodeNode::qualifiedName);
    }

    private int nodeKindRank(NodeKind kind) {
        return switch (kind) {
            case FILE -> 0;
            case PACKAGE, MODULE, NAMESPACE -> 1;
            case COMPONENT, ROUTE -> 2;
            case CLASS, INTERFACE, RECORD, ENUM -> 3;
            case METHOD, FUNCTION, CONSTRUCTOR -> 4;
            case IMPORT, EXPORT -> 5;
            default -> 8;
        };
    }

    private GraphVertex toVertex(CodeNode node, Map<Path, List<DetectedProblem>> problemsByPath) {
        return new GraphVertex(
                node.id(),
                shortLabel(node),
                node.kind(),
                node.qualifiedName(),
                node.filePath(),
                node.language(),
                node.line(),
                node.signature(),
                problemCount(node, problemsByPath)
        );
    }

    private String shortLabel(CodeNode node) {
        String name = node.name();
        if (name.length() <= 28) {
            return name;
        }
        return name.substring(0, 25) + "...";
    }

    private int problemCount(CodeNode node, Map<Path, List<DetectedProblem>> problemsByPath) {
        List<DetectedProblem> pathProblems = problemsByPath.getOrDefault(node.filePath(), List.of());
        if (pathProblems.isEmpty()) {
            return 0;
        }
        return pathProblems.size();
    }

    private boolean pathHasProblem(CodeNode node, Map<Path, List<DetectedProblem>> problemsByPath) {
        return problemsByPath.containsKey(node.filePath());
    }

    private Map<Path, List<DetectedProblem>> problemsByPath(List<DetectedProblem> problems) {
        if (problems == null || problems.isEmpty()) {
            return Map.of();
        }
        Map<Path, List<DetectedProblem>> result = new LinkedHashMap<>();
        for (DetectedProblem problem : problems) {
            if (problem.primaryPath() != null) {
                result.computeIfAbsent(problem.primaryPath(), ignored -> new ArrayList<>()).add(problem);
            }
        }
        return result;
    }
}
