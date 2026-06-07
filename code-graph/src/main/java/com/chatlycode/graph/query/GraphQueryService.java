package com.chatlycode.graph.query;

import com.chatlycode.graph.domain.CodeEdge;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.domain.EdgeKind;
import com.chatlycode.graph.domain.NodeKind;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class GraphQueryService {

    private final GraphTraverser traverser = new GraphTraverser();

    public List<CodeNode> searchSymbols(CodeGraph graph, String query) {
        return findByName(graph, query);
    }

    public List<CodeNode> findByName(CodeGraph graph, String query) {
        String normalizedQuery = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return graph.nodes().stream()
                .filter(node -> node.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || node.qualifiedName().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(Comparator.comparing(CodeNode::qualifiedName))
                .toList();
    }

    public CodeNode getNode(CodeGraph graph, String nodeId) {
        return graph.nodes().stream().filter(node -> node.id().equals(nodeId)).findFirst().orElse(null);
    }

    public List<CodeNode> findByKind(CodeGraph graph, NodeKind kind) {
        return graph.nodes().stream()
                .filter(node -> node.kind() == kind)
                .sorted(Comparator.comparing(CodeNode::qualifiedName))
                .toList();
    }

    public List<CodeNode> controllers(CodeGraph graph) {
        return graph.nodes().stream().filter(CodeNode::isController).sorted(Comparator.comparing(CodeNode::qualifiedName)).toList();
    }

    public List<CodeNode> services(CodeGraph graph) {
        return graph.nodes().stream().filter(CodeNode::isService).sorted(Comparator.comparing(CodeNode::qualifiedName)).toList();
    }

    public List<CodeNode> repositories(CodeGraph graph) {
        return graph.nodes().stream().filter(CodeNode::isRepository).sorted(Comparator.comparing(CodeNode::qualifiedName)).toList();
    }

    public List<String> outgoingDependencies(CodeGraph graph, CodeNode node) {
        Set<String> dependencies = new LinkedHashSet<>();
        for (CodeEdge edge : graph.edges()) {
            if (!edge.sourceId().equals(node.id())) {
                continue;
            }
            if (edge.kind() == EdgeKind.IMPORTS || edge.kind() == EdgeKind.REFERENCES || edge.kind() == EdgeKind.TYPE_OF) {
                dependencies.add(displayTarget(graph, edge.targetId()));
            }
        }
        return List.copyOf(dependencies);
    }

    public List<String> incomingDependencies(CodeGraph graph, CodeNode node) {
        Set<String> incoming = new LinkedHashSet<>();
        for (CodeEdge edge : graph.edges()) {
            if (!edge.targetId().equals(node.id())) {
                continue;
            }
            if (edge.kind() == EdgeKind.IMPORTS || edge.kind() == EdgeKind.REFERENCES || edge.kind() == EdgeKind.CALLS) {
                incoming.add(displayTarget(graph, edge.sourceId()));
            }
        }
        return List.copyOf(incoming);
    }

    public List<Path> fileDependencies(CodeGraph graph, Path filePath) {
        CodeNode fileNode = graph.nodes().stream()
                .filter(node -> node.kind() == NodeKind.FILE && node.filePath().equals(filePath))
                .findFirst()
                .orElse(null);
        if (fileNode == null) {
            return List.of();
        }
        Set<Path> dependencies = new LinkedHashSet<>();
        for (CodeEdge edge : graph.edges()) {
            if (edge.sourceId().equals(fileNode.id()) && edge.kind() == EdgeKind.IMPORTS) {
                graph.nodes().stream()
                        .filter(node -> node.id().equals(edge.targetId()))
                        .findFirst()
                        .ifPresent(node -> dependencies.add(node.filePath()));
            }
        }
        return List.copyOf(dependencies);
    }

    public List<CodeNode> impactRadius(CodeGraph graph, CodeNode node, int depth) {
        return traverser.impactRadius(graph, node.id(), depth);
    }

    public GraphAnswer answerQuestion(CodeGraph graph, String question) {
        String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
        if (normalized.contains("controller") && normalized.contains("database")) {
            return controllerToDatabaseFlow(graph, question);
        }
        if (normalized.contains("depend")) {
            return dependencyAnswer(graph, question, normalized);
        }
        if (normalized.contains("risky") || normalized.contains("blast")) {
            return riskyFilesAnswer(graph, question);
        }
        if (normalized.contains("service")) {
            return roleAnswer(graph, question, services(graph));
        }
        return genericSearchAnswer(graph, question);
    }

    private GraphAnswer controllerToDatabaseFlow(CodeGraph graph, String question) {
        List<String> evidence = new ArrayList<>();
        List<String> files = new ArrayList<>();
        for (CodeNode controller : controllers(graph)) {
            List<String> deps = outgoingDependencies(graph, controller);
            boolean touchesRepository = deps.stream().anyMatch(dep -> dep.contains("Repository") || dep.contains("repository"));
            if (touchesRepository) {
                evidence.add(controller.qualifiedName() + " -> " + deps);
                files.add(formatPath(controller.filePath()));
            }
        }
        String summary = evidence.isEmpty()
                ? "No direct controller-to-repository flow was found in graph evidence."
                : "Detected controller-to-persistence paths from imports and type dependencies.";
        return new GraphAnswer(question, summary, evidence, files, evidence.isEmpty());
    }

    private GraphAnswer dependencyAnswer(CodeGraph graph, String question, String normalized) {
        CodeNode node = findMentionedNode(graph, normalized);
        if (node == null) {
            return new GraphAnswer(question, "No graph node matched the question.", List.of(), List.of(), true);
        }
        List<String> evidence = new ArrayList<>();
        evidence.addAll(incomingDependencies(graph, node).stream().map(dep -> "Incoming: " + dep).toList());
        evidence.addAll(outgoingDependencies(graph, node).stream().map(dep -> "Outgoing: " + dep).toList());
        return new GraphAnswer(question, "Dependencies for " + node.qualifiedName(), evidence, List.of(formatPath(node.filePath())), false);
    }

    private CodeNode findMentionedNode(CodeGraph graph, String normalizedQuestion) {
        Set<String> tokens = Set.copyOf(significantTokens(normalizedQuestion));
        CodeNode exact = graph.nodes().stream()
                .filter(node -> tokens.contains(node.name().toLowerCase(Locale.ROOT))
                        || tokens.contains(node.qualifiedName().toLowerCase(Locale.ROOT)))
                .max(Comparator.comparingInt(node -> node.qualifiedName().length()))
                .orElse(null);
        if (exact != null) {
            return exact;
        }
        CodeNode direct = graph.nodes().stream()
                .filter(node -> normalizedQuestion.contains(node.name().toLowerCase(Locale.ROOT))
                        || normalizedQuestion.contains(node.qualifiedName().toLowerCase(Locale.ROOT)))
                .max(Comparator.comparingInt(node -> node.qualifiedName().length()))
                .orElse(null);
        if (direct != null) {
            return direct;
        }
        for (String token : significantTokens(normalizedQuestion)) {
            List<CodeNode> matches = findByName(graph, token);
            if (!matches.isEmpty()) {
                return matches.getFirst();
            }
        }
        return null;
    }

    private GraphAnswer riskyFilesAnswer(CodeGraph graph, String question) {
        List<String> evidence = new ArrayList<>();
        List<String> files = new ArrayList<>();
        for (CodeNode node : graph.nodes()) {
            int inbound = incomingDependencies(graph, node).size();
            if (inbound >= 8) {
                evidence.add(node.qualifiedName() + " inbound=" + inbound);
                files.add(formatPath(node.filePath()));
            }
        }
        return new GraphAnswer(
                question,
                files.isEmpty() ? "No high blast-radius nodes detected." : "High inbound dependency nodes",
                evidence,
                files,
                false
        );
    }

    private GraphAnswer roleAnswer(CodeGraph graph, String question, List<CodeNode> nodes) {
        List<String> evidence = nodes.stream().map(CodeNode::qualifiedName).limit(20).toList();
        List<String> files = nodes.stream().map(node -> formatPath(node.filePath())).distinct().limit(20).toList();
        return new GraphAnswer(question, "Found " + nodes.size() + " matching nodes", evidence, files, false);
    }

    private GraphAnswer genericSearchAnswer(CodeGraph graph, String question) {
        String token = extractToken(question == null ? "" : question.toLowerCase(Locale.ROOT));
        List<CodeNode> matches = findByName(graph, token);
        List<String> evidence = matches.stream().map(CodeNode::qualifiedName).limit(15).toList();
        List<String> files = matches.stream().map(node -> formatPath(node.filePath())).distinct().limit(15).toList();
        return new GraphAnswer(
                question,
                matches.isEmpty() ? "No graph matches found." : "Matched " + matches.size() + " nodes",
                evidence,
                files,
                matches.isEmpty()
        );
    }

    private String extractToken(String normalizedQuestion) {
        for (String token : significantTokens(normalizedQuestion)) {
            return token;
        }
        return normalizedQuestion;
    }

    private List<String> significantTokens(String normalizedQuestion) {
        String[] tokens = normalizedQuestion.replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() >= 3 && !isStopWord(token)) {
                result.add(token);
            }
        }
        return result;
    }

    private boolean isStopWord(String token) {
        return switch (token) {
            case "what", "which", "where", "show", "flow", "from", "this", "that", "files", "file", "class",
                 "depend", "depends", "dependency", "dependencies", "used", "uses", "using" -> true;
            default -> false;
        };
    }

    private String formatPath(Path path) {
        return path == null ? "" : path.toString().replace('\\', '/');
    }

    private String displayTarget(CodeGraph graph, String nodeIdOrName) {
        return graph.nodes().stream()
                .filter(node -> node.id().equals(nodeIdOrName))
                .findFirst()
                .map(node -> {
                    if (node.qualifiedName() != null && !node.qualifiedName().isBlank()) {
                        return node.qualifiedName();
                    }
                    if (node.filePath() != null) {
                        return formatPath(node.filePath());
                    }
                    return node.name();
                })
                .orElse(nodeIdOrName);
    }
}
