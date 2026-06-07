package com.chatlycode.architecture.application;

import com.chatlycode.architecture.domain.ArchitectureSummary;
import com.chatlycode.graph.domain.CodeGraph;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class ArchitectureAnalyzer {

    public ArchitectureSummary analyze(CodeGraph graph) {
        Map<String, Long> packageCounts = graph.nodes().stream()
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
                mermaid(topPackages)
        );
    }

    private String packageName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot <= 0 ? "" : qualifiedName.substring(0, lastDot);
    }

    private String mermaid(Iterable<String> packages) {
        StringBuilder builder = new StringBuilder("C4Container\n");
        builder.append("    title Chatly Code Project Architecture Draft\n");
        builder.append("    Person(dev, \"Developer\")\n");
        builder.append("    System_Boundary(project, \"Opened Project\") {\n");
        for (String packageName : packages) {
            String id = packageName.replaceAll("[^A-Za-z0-9]", "_");
            builder.append("        Container(").append(id).append(", \"").append(packageName).append("\", \"Java package\")\n");
        }
        builder.append("    }\n");
        builder.append("    Rel(dev, project, \"Inspects and changes\")\n");
        return builder.toString();
    }
}
