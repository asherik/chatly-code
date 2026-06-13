package com.chatlycode.graph.context;

import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.query.GraphQueryService;

import java.util.List;
import java.util.stream.Collectors;

public final class AgentContextBuilder {

    private final GraphQueryService graphQueryService = new GraphQueryService();

    public String build(CodeGraph graph, String focusPath) {
        StringBuilder builder = new StringBuilder();
        builder.append("Graph context\n");
        builder.append("- files: ").append(graph.files().size()).append('\n');
        builder.append("- nodes: ").append(graph.nodes().size()).append('\n');
        builder.append("- edges: ").append(graph.edges().size()).append('\n');
        if (focusPath != null && !focusPath.isBlank()) {
            builder.append("\nFocus file\n");
            graph.nodes().stream()
                    .filter(node -> node.filePath().toString().equals(focusPath))
                    .limit(30)
                    .forEach(node -> builder.append("- ").append(node.kind()).append(' ').append(node.qualifiedName())
                            .append(" [").append(node.startLine()).append("]\n"));
        }
        builder.append("\nControllers: ").append(join(graphQueryService.controllers(graph))).append('\n');
        builder.append("Services: ").append(join(graphQueryService.services(graph))).append('\n');
        builder.append("Repositories: ").append(join(graphQueryService.repositories(graph))).append('\n');
        return builder.toString();
    }

    private String join(List<CodeNode> nodes) {
        return nodes.stream().map(CodeNode::qualifiedName).limit(10).collect(Collectors.joining(", "));
    }
}
