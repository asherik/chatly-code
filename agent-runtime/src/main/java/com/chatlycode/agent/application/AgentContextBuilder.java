package com.chatlycode.agent.application;

import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.query.GraphQueryService;
import com.chatlycode.problem.domain.DetectedProblem;

import java.util.List;
import java.util.stream.Collectors;

public final class AgentContextBuilder {

    private static final int MAX_NODES = 40;
    private static final int MAX_PROBLEMS = 10;

    private final GraphQueryService graphQueryService = new GraphQueryService();

    public String build(CodeGraph graph, List<DetectedProblem> problems, String focusPath) {
        StringBuilder builder = new StringBuilder();
        builder.append("Project graph summary\n");
        builder.append("- files: ").append(graph.files().size()).append('\n');
        builder.append("- nodes: ").append(graph.nodes().size()).append('\n');
        builder.append("- edges: ").append(graph.edges().size()).append('\n');
        builder.append("- unresolved references: ").append(graph.unresolvedReferences().size()).append('\n');

        if (focusPath != null && !focusPath.isBlank()) {
            builder.append("\nFocus file nodes\n");
            graph.nodes().stream()
                    .filter(node -> node.filePath().toString().equals(focusPath))
                    .limit(MAX_NODES)
                    .forEach(node -> builder.append("- ").append(node.kind()).append(' ').append(node.qualifiedName()).append('\n'));
        }

        builder.append("\nTop problems\n");
        problems.stream()
                .limit(MAX_PROBLEMS)
                .forEach(problem -> builder.append("- [")
                        .append(problem.severity())
                        .append("] ")
                        .append(problem.title())
                        .append(" @ ")
                        .append(problem.primaryPath())
                        .append('\n'));

        builder.append("\nControllers\n");
        appendRoleSummary(builder, graphQueryService.controllers(graph));
        builder.append("\nServices\n");
        appendRoleSummary(builder, graphQueryService.services(graph));
        builder.append("\nRepositories\n");
        appendRoleSummary(builder, graphQueryService.repositories(graph));
        return builder.toString();
    }

    private void appendRoleSummary(StringBuilder builder, List<CodeNode> nodes) {
        String summary = nodes.stream()
                .map(CodeNode::qualifiedName)
                .limit(15)
                .collect(Collectors.joining(", "));
        builder.append(summary.isBlank() ? "- none" : summary).append('\n');
    }
}
