package com.chatlycode.graph.query;

import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;

import java.util.Comparator;
import java.util.List;

public final class GraphQueryService {

    public List<CodeNode> findByName(CodeGraph graph, String query) {
        String normalizedQuery = query == null ? "" : query.toLowerCase();
        return graph.nodes().stream()
                .filter(node -> node.name().toLowerCase().contains(normalizedQuery)
                        || node.qualifiedName().toLowerCase().contains(normalizedQuery))
                .sorted(Comparator.comparing(CodeNode::qualifiedName))
                .toList();
    }
}
