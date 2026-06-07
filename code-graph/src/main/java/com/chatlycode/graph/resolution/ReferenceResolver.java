package com.chatlycode.graph.resolution;

import com.chatlycode.graph.domain.CodeEdge;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.domain.CodeUnresolvedReference;
import com.chatlycode.graph.domain.EdgeKind;
import com.chatlycode.graph.domain.EdgeProvenance;
import com.chatlycode.graph.domain.NodeKind;
import com.chatlycode.graph.mapper.GraphEdgeMapper;
import com.chatlycode.language.spi.UnresolvedReference;
import com.chatlycode.shared.id.Ids;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ReferenceResolver {

    private final GraphEdgeMapper edgeMapper = new GraphEdgeMapper();

    public ResolutionResult resolve(List<CodeNode> nodes, List<CodeEdge> edges, List<UnresolvedReference> unresolvedReferences) {
        Map<String, CodeNode> nodesById = new HashMap<>();
        Map<String, List<CodeNode>> byQualifiedName = new HashMap<>();
        Map<String, List<CodeNode>> bySimpleName = new HashMap<>();
        for (CodeNode node : nodes) {
            nodesById.put(node.id(), node);
            byQualifiedName.computeIfAbsent(node.qualifiedName(), ignored -> new ArrayList<>()).add(node);
            bySimpleName.computeIfAbsent(node.name(), ignored -> new ArrayList<>()).add(node);
        }

        List<CodeEdge> resolvedEdges = new ArrayList<>();
        List<CodeUnresolvedReference> unresolved = new ArrayList<>();

        for (CodeEdge edge : edges) {
            if (nodesById.containsKey(edge.targetId())) {
                resolvedEdges.add(edge);
                continue;
            }
            resolveTarget(edge.targetId(), edge.sourceId(), byQualifiedName, bySimpleName, nodesById)
                    .ifPresentOrElse(
                            targetId -> resolvedEdges.add(replaceTarget(edge, targetId)),
                            () -> {
                                unresolved.add(toUnresolved(edge));
                                resolvedEdges.add(edge);
                            }
                    );
        }

        for (UnresolvedReference reference : unresolvedReferences) {
            resolveTarget(reference.referenceName(), reference.fromNodeId(), byQualifiedName, bySimpleName, nodesById)
                    .ifPresentOrElse(
                            targetId -> resolvedEdges.add(edgeMapper.references(
                                    reference.fromNodeId(),
                                    targetId,
                                    reference.line(),
                                    0.75
                            )),
                            () -> unresolved.add(new CodeUnresolvedReference(
                                    Ids.newId("unresolved"),
                                    reference.fromNodeId(),
                                    reference.referenceName(),
                                    reference.referenceKind(),
                                    reference.relativePath(),
                                    "unknown",
                                    reference.line(),
                                    reference.candidates()
                            ))
                    );
        }

        resolvedEdges.addAll(resolveImportTargets(nodes, byQualifiedName));
        return new ResolutionResult(List.copyOf(resolvedEdges), List.copyOf(unresolved));
    }

    private List<CodeEdge> resolveImportTargets(List<CodeNode> nodes, Map<String, List<CodeNode>> byQualifiedName) {
        List<CodeEdge> importEdges = new ArrayList<>();
        for (CodeNode node : nodes) {
            if (node.kind() != NodeKind.IMPORT) {
                continue;
            }
            String importName = node.qualifiedName();
            if (importName.endsWith(".*")) {
                continue;
            }
            byQualifiedName.getOrDefault(importName, List.of()).stream()
                    .filter(target -> target.kind() == NodeKind.CLASS || target.kind() == NodeKind.INTERFACE || target.kind() == NodeKind.RECORD)
                    .findFirst()
                    .ifPresent(target -> importEdges.add(edgeMapper.imports(node.id(), target.id(), node.startLine())));
        }
        return importEdges;
    }

    private Optional<String> resolveTarget(
            String referenceName,
            String sourceId,
            Map<String, List<CodeNode>> byQualifiedName,
            Map<String, List<CodeNode>> bySimpleName,
            Map<String, CodeNode> nodesById
    ) {
        if (nodesById.containsKey(referenceName)) {
            return Optional.of(referenceName);
        }
        List<CodeNode> qualifiedMatches = byQualifiedName.get(referenceName);
        if (qualifiedMatches != null && !qualifiedMatches.isEmpty()) {
            return Optional.of(qualifiedMatches.getFirst().id());
        }
        String simpleName = referenceName.contains(".")
                ? referenceName.substring(referenceName.lastIndexOf('.') + 1)
                : referenceName;
        List<CodeNode> simpleMatches = bySimpleName.get(simpleName);
        if (simpleMatches != null && !simpleMatches.isEmpty()) {
            return Optional.of(simpleMatches.getFirst().id());
        }
        return Optional.empty();
    }

    private CodeEdge replaceTarget(CodeEdge edge, String targetId) {
        return new CodeEdge(
                edge.sourceId(),
                targetId,
                edge.kind(),
                EdgeProvenance.LANGUAGE_RESOLVER,
                Math.max(edge.confidence(), 0.8),
                edge.line(),
                edge.metadataJson()
        );
    }

    private CodeUnresolvedReference toUnresolved(CodeEdge edge) {
        return new CodeUnresolvedReference(
                Ids.newId("unresolved"),
                edge.sourceId(),
                edge.targetId(),
                edge.kind().name().toLowerCase(Locale.ROOT),
                Path.of(""),
                "unknown",
                edge.line() == null ? 1 : edge.line(),
                List.of()
        );
    }

    public record ResolutionResult(List<CodeEdge> edges, List<CodeUnresolvedReference> unresolvedReferences) {
    }
}
