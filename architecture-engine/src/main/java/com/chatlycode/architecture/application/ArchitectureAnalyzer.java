package com.chatlycode.architecture.application;

import com.chatlycode.architecture.domain.ArchitectureSummary;
import com.chatlycode.graph.domain.CodeEdge;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.domain.EdgeKind;
import com.chatlycode.graph.domain.NodeKind;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ArchitectureAnalyzer {

    public ArchitectureSummary analyze(CodeGraph graph) {
        Map<String, Long> packageCounts = graph.nodes().stream()
                .filter(this::isArchitectureNode)
                .map(this::moduleName)
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
                structurizrDsl(graph, topPackages)
        );
    }

    private boolean isArchitectureNode(CodeNode node) {
        return node.kind() == NodeKind.MODULE || node.kind() == NodeKind.PACKAGE || node.kind() == NodeKind.NAMESPACE
                || node.kind() == NodeKind.CLASS || node.kind() == NodeKind.INTERFACE || node.kind() == NodeKind.RECORD
                || node.kind() == NodeKind.COMPONENT || node.kind() == NodeKind.FUNCTION;
    }

    private String moduleName(CodeNode node) {
        String pathModule = pathModule(node.filePath());
        if (!pathModule.isBlank()) {
            return pathModule;
        }
        String qualifiedName = node.qualifiedName();
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot <= 0 ? "" : qualifiedName.substring(0, lastDot);
    }

    private String pathModule(Path path) {
        if (path == null) {
            return "";
        }
        String normalized = path.toString().replace('\\', '/');
        String[] parts = normalized.split("/");
        if (parts.length >= 3 && parts[1].equals("src")) {
            return parts[0] + "/src/" + parts[2];
        }
        if (parts.length >= 4 && parts[0].equals("packages")) {
            return parts[0] + "/" + parts[1] + "/" + parts[2];
        }
        if (parts.length >= 3 && parts[0].equals("shared")) {
            return parts[0] + "/" + parts[1] + "/" + parts[2];
        }
        return parts.length == 0 ? "" : parts[0];
    }

    private String structurizrDsl(CodeGraph graph, List<String> modules) {
        Map<String, String> idsByModule = new LinkedHashMap<>();
        for (String module : modules) {
            idsByModule.put(module, sanitize(module));
        }
        StringBuilder builder = new StringBuilder();
        builder.append("workspace \"").append(escape(projectName(graph))).append("\" \"Architecture inferred from the indexed code graph.\" {\n\n");
        builder.append("    !identifiers hierarchical\n\n");
        builder.append("    model {\n");
        builder.append("        user = person \"Developer\"\n");
        builder.append("        project = softwareSystem \"").append(escape(projectName(graph))).append("\" {\n");
        for (String module : modules) {
            String technology = technologyFor(module);
            String tags = tagsFor(module);
            builder.append("            ").append(idsByModule.get(module)).append(" = container \"")
                    .append(escape(module)).append("\" \"").append(descriptionFor(module)).append("\" \"")
                    .append(technology).append("\"");
            if (!tags.isBlank()) {
                builder.append(" {\n");
                builder.append("                tags \"").append(tags).append("\"\n");
                builder.append("            }\n");
            } else {
                builder.append("\n");
            }
        }
        builder.append("        }\n\n");
        builder.append("        user -> project \"Inspects and changes\"\n");
        relationships(graph, idsByModule).forEach(relationship -> builder.append("        ").append(relationship).append('\n'));
        builder.append("    }\n\n");
        builder.append("    views {\n");
        builder.append("        systemContext project \"SystemContext\" {\n");
        builder.append("            include *\n");
        builder.append("            autoLayout lr\n");
        builder.append("        }\n\n");
        builder.append("        container project \"Containers\" {\n");
        builder.append("            include *\n");
        builder.append("            autoLayout lr\n");
        builder.append("        }\n\n");
        builder.append("        styles {\n");
        builder.append("            element \"Element\" {\n");
        builder.append("                color #0f172a\n");
        builder.append("                stroke #2563eb\n");
        builder.append("                strokeWidth 3\n");
        builder.append("                shape roundedbox\n");
        builder.append("            }\n");
        builder.append("            element \"Person\" {\n");
        builder.append("                shape person\n");
        builder.append("                background #f8fafc\n");
        builder.append("            }\n");
        builder.append("            element \"Database\" {\n");
        builder.append("                shape cylinder\n");
        builder.append("                background #dbeafe\n");
        builder.append("            }\n");
        builder.append("            element \"UI\" {\n");
        builder.append("                background #dcfce7\n");
        builder.append("            }\n");
        builder.append("            element \"Core\" {\n");
        builder.append("                background #fef3c7\n");
        builder.append("            }\n");
        builder.append("            relationship \"Relationship\" {\n");
        builder.append("                thickness 3\n");
        builder.append("            }\n");
        builder.append("        }\n");
        builder.append("    }\n\n");
        builder.append("    configuration {\n");
        builder.append("        scope softwaresystem\n");
        builder.append("    }\n");
        builder.append("}\n");
        return builder.toString();
    }

    private List<String> relationships(CodeGraph graph, Map<String, String> idsByModule) {
        Map<String, CodeNode> nodesById = graph.nodes().stream().collect(Collectors.toMap(CodeNode::id, node -> node, (a, b) -> a));
        Map<String, Long> counts = new LinkedHashMap<>();
        for (CodeEdge edge : graph.edges()) {
            if (edge.kind() != EdgeKind.IMPORTS && edge.kind() != EdgeKind.REFERENCES && edge.kind() != EdgeKind.CALLS) {
                continue;
            }
            CodeNode source = nodesById.get(edge.sourceId());
            if (source == null) {
                continue;
            }
            String sourceModule = moduleName(source);
            String targetModule = Optional.ofNullable(nodesById.get(edge.targetId()))
                    .map(this::moduleName)
                    .orElseGet(() -> inferTargetModule(edge.targetId(), idsByModule.keySet()));
            if (!idsByModule.containsKey(sourceModule) || !idsByModule.containsKey(targetModule) || sourceModule.equals(targetModule)) {
                continue;
            }
            String key = idsByModule.get(sourceModule) + " -> " + idsByModule.get(targetModule);
            counts.merge(key, 1L, Long::sum);
        }
        List<String> result = new ArrayList<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .limit(16)
                .forEach(entry -> result.add(entry.getKey() + " \"Depends on\" \"edges: " + entry.getValue() + "\""));
        if (result.isEmpty()) {
            result.addAll(heuristicRelationships(idsByModule));
        }
        return result;
    }

    private List<String> heuristicRelationships(Map<String, String> idsByModule) {
        List<String> result = new ArrayList<>();
        for (String source : idsByModule.keySet()) {
            for (String target : idsByModule.keySet()) {
                if (source.equals(target)) {
                    continue;
                }
                if ((isDesktopOrUi(source) && isCore(target))
                        || (isProviderOrTool(source) && isStorage(target))) {
                    result.add(idsByModule.get(source) + " -> " + idsByModule.get(target) + " \"Depends on\" \"inferred from module boundaries\"");
                }
            }
        }
        return result.stream().distinct().limit(16).toList();
    }

    private boolean isDesktopOrUi(String module) {
        String lower = module.toLowerCase();
        return lower.contains("desktop") || lower.contains("ui") || lower.contains("component") || lower.contains("site");
    }

    private boolean isCore(String module) {
        return module.toLowerCase().startsWith("core");
    }

    private boolean isStorage(String module) {
        String lower = module.toLowerCase();
        return lower.contains("storage") || lower.contains("database");
    }

    private boolean isProviderOrTool(String module) {
        String lower = module.toLowerCase();
        return lower.contains("provider") || lower.contains("tool");
    }

    private String inferTargetModule(String targetId, Iterable<String> modules) {
        String normalizedTarget = normalizeDependencyName(targetId);
        for (String module : modules) {
            String normalizedModule = normalizeDependencyName(module);
            if (!normalizedModule.isBlank() && normalizedTarget.contains(normalizedModule)) {
                return module;
            }
            if (normalizedModule.isBlank()) {
                continue;
            }
            String lastSegment = normalizedModule.substring(normalizedModule.lastIndexOf('/') + 1);
            if (lastSegment.length() >= 4 && normalizedTarget.contains(lastSegment)) {
                return module;
            }
        }
        return "";
    }

    private String normalizeDependencyName(String value) {
        return value == null ? "" : value.toLowerCase()
                .replace("::", "/")
                .replace(".", "/")
                .replace("\\", "/")
                .replaceAll("[^a-z0-9/]", "");
    }

    private String projectName(CodeGraph graph) {
        return graph.projectId() == null ? "Opened Project" : graph.projectId().value();
    }

    private String technologyFor(String module) {
        if (module.contains("ui") || module.contains("packages/")) {
            return "TypeScript";
        }
        if (module.contains("storage") || module.contains("dto")) {
            return "Shared model";
        }
        return "Code module";
    }

    private String tagsFor(String module) {
        String lower = module.toLowerCase();
        if (lower.contains("storage") || lower.contains("database")) {
            return "Database";
        }
        if (lower.contains("ui") || lower.contains("page") || lower.contains("component")) {
            return "UI";
        }
        if (lower.startsWith("core")) {
            return "Core";
        }
        return "";
    }

    private String descriptionFor(String module) {
        String lower = module.toLowerCase();
        if (lower.contains("storage")) {
            return "Persistence and storage integration";
        }
        if (lower.contains("ui") || lower.contains("component")) {
            return "User interface module";
        }
        if (lower.contains("dto") || lower.contains("types")) {
            return "Shared data contracts";
        }
        return "Indexed code module";
    }

    private String sanitize(String value) {
        String sanitized = value.replaceAll("[^A-Za-z0-9]", "_");
        if (sanitized.isBlank()) {
            return "module";
        }
        if (Character.isDigit(sanitized.charAt(0))) {
            return "m_" + sanitized;
        }
        return sanitized;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
