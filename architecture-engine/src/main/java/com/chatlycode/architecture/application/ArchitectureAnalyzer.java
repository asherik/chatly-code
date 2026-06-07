package com.chatlycode.architecture.application;

import com.chatlycode.architecture.domain.ArchitectureSummary;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.domain.NodeKind;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class ArchitectureAnalyzer {

    public ArchitectureSummary analyze(CodeGraph graph) {
        Map<String, Long> packageCounts = graph.nodes().stream()
                .filter(node -> node.kind() == NodeKind.CLASS || node.kind() == NodeKind.INTERFACE || node.kind() == NodeKind.RECORD)
                .map(node -> packageName(node.qualifiedName()))
                .filter(packageName -> !packageName.isBlank())
                .collect(Collectors.groupingBy(packageName -> packageName, LinkedHashMap::new, Collectors.counting()));

        var topPackages = packageCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .limit(8)
                .map(Map.Entry::getKey)
                .toList();

        return new ArchitectureSummary(
                graph.files().size(),
                graph.nodes().size(),
                graph.edges().size(),
                topPackages,
                mermaid(graph, topPackages)
        );
    }

    private String packageName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot <= 0 ? "" : qualifiedName.substring(0, lastDot);
    }

    private String mermaid(CodeGraph graph, Iterable<String> packages) {
        StringBuilder builder = new StringBuilder("C4Container\n");
        builder.append("    title Chatly Code Project Architecture Draft\n");
        builder.append("    Person(dev, \"Developer\")\n");
        builder.append("    System_Boundary(project, \"Opened Project\") {\n");
        for (String packageName : packages) {
            String id = sanitize(packageName);
            builder.append("        Container(").append(id).append(", \"").append(packageName).append("\", \"Java package\")\n");
        }
        appendLayerContainer(builder, "controllers", graph.nodes().stream().filter(CodeNode::isController).count(), "Controllers");
        appendLayerContainer(builder, "services", graph.nodes().stream().filter(CodeNode::isService).count(), "Services");
        appendLayerContainer(builder, "repositories", graph.nodes().stream().filter(CodeNode::isRepository).count(), "Repositories");
        builder.append("    }\n");
        builder.append("    Rel(dev, project, \"Inspects and changes\")\n");
        if (graph.nodes().stream().anyMatch(CodeNode::isController) && graph.nodes().stream().anyMatch(CodeNode::isService)) {
            builder.append("    Rel(controllers, services, \"Uses\")\n");
        }
        if (graph.nodes().stream().anyMatch(CodeNode::isService) && graph.nodes().stream().anyMatch(CodeNode::isRepository)) {
            builder.append("    Rel(services, repositories, \"Uses\")\n");
        }
        return builder.toString();
    }

    private void appendLayerContainer(StringBuilder builder, String id, long count, String label) {
        if (count > 0) {
            builder.append("        Container(").append(id).append(", \"").append(label).append("\", \"")
                    .append(count).append(" types\")\n");
        }
    }

    private String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9]", "_");
    }
}
