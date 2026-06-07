package com.chatlycode.graph.query;

import com.chatlycode.graph.domain.CodeEdge;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.domain.EdgeKind;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class GraphTraverser {

    public List<CodeNode> ancestors(CodeGraph graph, String nodeId) {
        Map<String, CodeNode> nodesById = indexNodes(graph);
        List<CodeNode> ancestors = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        collectAncestors(graph, nodeId, nodesById, ancestors, visited);
        return List.copyOf(ancestors);
    }

    public List<CodeNode> children(CodeGraph graph, String nodeId) {
        List<CodeNode> children = new ArrayList<>();
        for (CodeEdge edge : graph.edges()) {
            if (edge.kind() != EdgeKind.CONTAINS || !edge.sourceId().equals(nodeId)) {
                continue;
            }
            graph.nodes().stream()
                    .filter(node -> node.id().equals(edge.targetId()))
                    .findFirst()
                    .ifPresent(children::add);
        }
        return List.copyOf(children);
    }

    public List<CodeNode> impactRadius(CodeGraph graph, String nodeId, int depth) {
        Map<String, CodeNode> nodesById = indexNodes(graph);
        List<CodeNode> impacted = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        collectImpact(graph, nodeId, depth, nodesById, impacted, visited);
        return List.copyOf(impacted);
    }

    private void collectAncestors(
            CodeGraph graph,
            String nodeId,
            Map<String, CodeNode> nodesById,
            List<CodeNode> ancestors,
            Set<String> visited
    ) {
        for (CodeEdge edge : graph.edges()) {
            if (edge.kind() != EdgeKind.CONTAINS || !edge.targetId().equals(nodeId) || !visited.add(edge.sourceId())) {
                continue;
            }
            CodeNode parent = nodesById.get(edge.sourceId());
            if (parent != null) {
                ancestors.add(parent);
                collectAncestors(graph, parent.id(), nodesById, ancestors, visited);
            }
        }
    }

    private void collectImpact(
            CodeGraph graph,
            String nodeId,
            int depth,
            Map<String, CodeNode> nodesById,
            List<CodeNode> impacted,
            Set<String> visited
    ) {
        if (depth < 0 || !visited.add(nodeId)) {
            return;
        }
        CodeNode node = nodesById.get(nodeId);
        if (node != null) {
            impacted.add(node);
        }
        for (CodeEdge edge : graph.edges()) {
            if (!edge.targetId().equals(nodeId)) {
                continue;
            }
            if (edge.kind() == EdgeKind.IMPORTS || edge.kind() == EdgeKind.REFERENCES || edge.kind() == EdgeKind.CALLS) {
                collectImpact(graph, edge.sourceId(), depth - 1, nodesById, impacted, visited);
            }
        }
    }

    private Map<String, CodeNode> indexNodes(CodeGraph graph) {
        return graph.nodes().stream().collect(Collectors.toMap(CodeNode::id, Function.identity(), (left, right) -> left));
    }
}
