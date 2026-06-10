package com.chatlycode.architecture.application;

import com.chatlycode.architecture.domain.ArchitectureSummary;
import com.chatlycode.architecture.domain.ArchitectureContainer;
import com.chatlycode.architecture.domain.ArchitectureRelationship;
import com.chatlycode.graph.domain.CodeEdge;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.domain.EdgeKind;
import com.chatlycode.graph.domain.NodeKind;
import com.structurizr.dsl.StructurizrDslParser;
import com.structurizr.io.json.JsonWriter;
import com.structurizr.view.ComponentView;
import com.structurizr.view.ContainerView;
import com.structurizr.view.Dimensions;
import com.structurizr.view.ElementView;
import com.structurizr.view.ModelView;
import com.structurizr.view.RelationshipView;
import com.structurizr.view.SystemContextView;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

        Map<String, String> idsByModule = idsByModule(topPackages);
        List<ArchitectureContainer> containers = containers(topPackages, idsByModule);
        List<ArchitectureRelationship> relationships = relationships(graph, idsByModule);
        Map<String, List<ComponentCandidate>> componentsByContainerId = componentsByContainerId(graph, idsByModule);
        String structurizrDsl = structurizrDsl(graph, containers, relationships, componentsByContainerId);

        return new ArchitectureSummary(
                graph.files().size(),
                graph.nodes().size(),
                graph.edges().size(),
                topPackages,
                containers,
                relationships,
                structurizrDsl,
                structurizrJson(structurizrDsl)
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

    private Map<String, String> idsByModule(List<String> modules) {
        Map<String, String> idsByModule = new LinkedHashMap<>();
        for (String module : modules) {
            idsByModule.put(module, uniqueId(sanitize(module), idsByModule));
        }
        return idsByModule;
    }

    private String uniqueId(String candidate, Map<String, String> existing) {
        if (!existing.containsValue(candidate)) {
            return candidate;
        }
        int suffix = 2;
        while (existing.containsValue(candidate + "_" + suffix)) {
            suffix++;
        }
        return candidate + "_" + suffix;
    }

    private List<ArchitectureContainer> containers(List<String> modules, Map<String, String> idsByModule) {
        return modules.stream()
                .map(module -> new ArchitectureContainer(
                        idsByModule.get(module),
                        module,
                        descriptionFor(module),
                        technologyFor(module),
                        tagFor(module)
                ))
                .toList();
    }

    private String structurizrDsl(
            CodeGraph graph,
            List<ArchitectureContainer> containers,
            List<ArchitectureRelationship> relationships,
            Map<String, List<ComponentCandidate>> componentsByContainerId
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("workspace \"").append(escape(projectName(graph))).append("\" \"Architecture inferred from the indexed code graph.\" {\n\n");
        builder.append("    model {\n");
        builder.append("        user = person \"Developer\" \"Maintains and evolves the project\"\n");
        builder.append("        project = softwareSystem \"").append(escape(projectName(graph))).append("\" \"Indexed local codebase\" {\n");
        for (ArchitectureContainer container : containers) {
            builder.append("            ").append(container.id()).append(" = container \"")
                    .append(escape(container.name())).append("\" \"").append(escape(container.description())).append("\" \"")
                    .append(escape(container.technology())).append("\"");
            List<ComponentCandidate> components = componentsByContainerId.getOrDefault(container.id(), List.of());
            if (!container.tag().isBlank() || !components.isEmpty()) {
                builder.append(" {\n");
                if (!container.tag().isBlank()) {
                    builder.append("                tags \"").append(escape(container.tag())).append("\"\n");
                }
                for (ComponentCandidate component : components) {
                    builder.append("                ").append(component.id()).append(" = component \"")
                            .append(escape(component.name())).append("\" \"")
                            .append(escape(component.description())).append("\" \"")
                            .append(escape(component.technology())).append("\" {\n");
                    builder.append("                    tags \"Component\"\n");
                    builder.append("                }\n");
                }
                builder.append("            }\n");
            } else {
                builder.append("\n");
            }
        }
        builder.append("        }\n\n");
        builder.append("        user -> project \"Inspects and changes\"\n");
        relationships.forEach(relationship -> builder.append("        ")
                .append(relationship.sourceId()).append(" -> ").append(relationship.targetId())
                .append(" \"").append(escape(relationship.description())).append("\"")
                .append('\n'));
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
        for (ArchitectureContainer container : containers) {
            if (componentsByContainerId.getOrDefault(container.id(), List.of()).isEmpty()) {
                continue;
            }
            builder.append("        component ").append(container.id()).append(" \"Components_")
                    .append(container.id()).append("\" {\n");
            builder.append("            include *\n");
            builder.append("            autoLayout lr\n");
            builder.append("        }\n\n");
        }
        builder.append("        styles {\n");
        builder.append("            element \"Element\" {\n");
        builder.append("                color #0f172a\n");
        builder.append("                stroke #2563eb\n");
        builder.append("                strokeWidth 3\n");
        builder.append("                width 260\n");
        builder.append("                height 130\n");
        builder.append("                fontSize 22\n");
        builder.append("                shape RoundedBox\n");
        builder.append("            }\n");
        builder.append("            element \"Person\" {\n");
                builder.append("                shape Person\n");
        builder.append("                background #f8fafc\n");
        builder.append("                width 210\n");
        builder.append("                height 110\n");
        builder.append("            }\n");
        builder.append("            element \"Database\" {\n");
        builder.append("                shape Cylinder\n");
        builder.append("                background #dbeafe\n");
        builder.append("                width 260\n");
        builder.append("                height 140\n");
        builder.append("            }\n");
        builder.append("            element \"UI\" {\n");
        builder.append("                background #dcfce7\n");
        builder.append("            }\n");
        builder.append("            element \"Core\" {\n");
        builder.append("                background #fef3c7\n");
        builder.append("            }\n");
        builder.append("            element \"Component\" {\n");
        builder.append("                background #f8fafc\n");
        builder.append("                stroke #64748b\n");
        builder.append("                width 250\n");
        builder.append("                height 120\n");
        builder.append("                fontSize 18\n");
        builder.append("                shape RoundedBox\n");
        builder.append("            }\n");
        builder.append("            relationship \"Relationship\" {\n");
        builder.append("                color #64748b\n");
        builder.append("                dashed true\n");
        builder.append("                thickness 2\n");
        builder.append("            }\n");
        builder.append("        }\n");
        builder.append("    }\n\n");
        builder.append("    configuration {\n");
        builder.append("        scope softwareSystem\n");
        builder.append("    }\n");
        builder.append("}\n");
        return builder.toString();
    }

    private String structurizrJson(String dsl) {
        try {
            StructurizrDslParser parser = new StructurizrDslParser();
            parser.parse(dsl);
            applyManualLayout(parser.getWorkspace());
            StringWriter writer = new StringWriter();
            new JsonWriter(true).write(parser.getWorkspace(), writer);
            return writer.toString();
        } catch (Exception exception) {
            return "";
        }
    }

    private void applyManualLayout(com.structurizr.Workspace workspace) {
        workspace.getViews().getSystemContextViews().forEach(this::layoutSystemContextView);
        workspace.getViews().getContainerViews().forEach(this::layoutContainerView);
        workspace.getViews().getComponentViews().forEach(this::layoutComponentView);
    }

    private void layoutSystemContextView(SystemContextView view) {
        view.disableAutomaticLayout();
        view.setDimensions(new Dimensions(900, 520));
        for (ElementView element : view.getElements()) {
            if (isPerson(element)) {
                element.setX(80);
                element.setY(205);
            } else {
                element.setX(430);
                element.setY(190);
            }
        }
        routeRelationships(view);
    }

    private void layoutContainerView(ContainerView view) {
        view.disableAutomaticLayout();
        view.setDimensions(new Dimensions(1420, 860));
        List<ElementView> people = sortedElements(view.getElements()).stream().filter(this::isPerson).toList();
        List<ElementView> ui = sortedElements(view.getElements()).stream().filter(element -> !isPerson(element) && isUi(element)).toList();
        List<ElementView> core = sortedElements(view.getElements()).stream().filter(element -> !isPerson(element) && !isUi(element) && !isStorage(element)).toList();
        List<ElementView> storage = sortedElements(view.getElements()).stream().filter(element -> !isPerson(element) && isStorage(element)).toList();

        placeColumn(people, 60, 350, 190);
        placeColumn(ui, 340, 140, 185);
        placeColumn(core, 690, 90, 170);
        placeColumn(storage, 1080, 220, 190);
        routeRelationships(view);
    }

    private void layoutComponentView(ComponentView view) {
        view.disableAutomaticLayout();
        List<ElementView> elements = sortedElements(view.getElements());
        int columns = Math.min(3, Math.max(1, elements.size()));
        int rows = (int) Math.ceil(elements.size() / (double) columns);
        int width = Math.max(900, 120 + columns * 300);
        int height = Math.max(520, 110 + rows * 180);
        view.setDimensions(new Dimensions(width, height));
        for (int index = 0; index < elements.size(); index++) {
            ElementView element = elements.get(index);
            int column = index % columns;
            int row = index / columns;
            element.setX(70 + column * 300);
            element.setY(70 + row * 180);
        }
        routeRelationships(view);
    }

    private void placeColumn(List<ElementView> elements, int x, int startY, int gapY) {
        for (int index = 0; index < elements.size(); index++) {
            ElementView element = elements.get(index);
            element.setX(x);
            element.setY(startY + index * gapY);
        }
    }

    private List<ElementView> sortedElements(Set<ElementView> elements) {
        return elements.stream()
                .sorted(Comparator.comparing(element -> element.getElement().getName()))
                .toList();
    }

    private void routeRelationships(ModelView view) {
        int index = 0;
        for (RelationshipView relationship : view.getRelationships()) {
            relationship.setRouting(com.structurizr.view.Routing.Orthogonal);
            relationship.setPosition(50);
            relationship.setVertices(List.of());
            relationship.setOrder(String.format("%03d", index++));
        }
    }

    private boolean isPerson(ElementView element) {
        return element.getElement().hasTag("Person");
    }

    private boolean isUi(ElementView element) {
        String name = element.getElement().getName().toLowerCase();
        return element.getElement().hasTag("UI")
                || name.contains("desktop")
                || name.contains("site")
                || name.contains("component")
                || name.contains("ui");
    }

    private boolean isStorage(ElementView element) {
        String name = element.getElement().getName().toLowerCase();
        return element.getElement().hasTag("Database")
                || name.contains("storage")
                || name.contains("database");
    }

    private List<ArchitectureRelationship> relationships(CodeGraph graph, Map<String, String> idsByModule) {
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
        List<ArchitectureRelationship> result = new ArrayList<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .limit(16)
                .forEach(entry -> {
                    String[] ids = entry.getKey().split(" -> ", 2);
                    result.add(new ArchitectureRelationship(ids[0], ids[1], "Depends on", ""));
                });
        if (result.isEmpty()) {
            result.addAll(heuristicRelationships(idsByModule));
        }
        return result;
    }

    private List<ArchitectureRelationship> heuristicRelationships(Map<String, String> idsByModule) {
        List<ArchitectureRelationship> result = new ArrayList<>();
        for (String source : idsByModule.keySet()) {
            for (String target : idsByModule.keySet()) {
                if (source.equals(target)) {
                    continue;
                }
                if ((isDesktopOrUi(source) && isCore(target))
                        || (isProviderOrTool(source) && isStorage(target))) {
                    result.add(new ArchitectureRelationship(
                            idsByModule.get(source),
                            idsByModule.get(target),
                            "Depends on",
                            ""
                    ));
                }
            }
        }
        return result.stream().distinct().limit(16).toList();
    }

    private Map<String, List<ComponentCandidate>> componentsByContainerId(CodeGraph graph, Map<String, String> idsByModule) {
        Map<String, List<CodeNode>> nodesByModule = new LinkedHashMap<>();
        for (CodeNode node : graph.nodes()) {
            if (!isComponentCandidate(node)) {
                continue;
            }
            String module = moduleName(node);
            String containerId = idsByModule.get(module);
            if (containerId == null || containerId.isBlank()) {
                continue;
            }
            nodesByModule.computeIfAbsent(containerId, ignored -> new ArrayList<>()).add(node);
        }

        Map<String, List<ComponentCandidate>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<CodeNode>> entry : nodesByModule.entrySet()) {
            Map<String, String> usedIds = new HashMap<>();
            List<ComponentCandidate> candidates = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(this::componentRank).thenComparing(CodeNode::qualifiedName))
                    .limit(9)
                    .map(node -> {
                        String id = uniqueId(sanitize(entry.getKey() + "_" + node.name()), usedIds);
                        usedIds.put(id, id);
                        return new ComponentCandidate(
                                id,
                                shortName(node.name()),
                                componentDescription(node),
                                componentTechnology(node)
                        );
                    })
                    .toList();
            result.put(entry.getKey(), candidates);
        }
        return result;
    }

    private boolean isComponentCandidate(CodeNode node) {
        return node.kind() == NodeKind.CLASS
                || node.kind() == NodeKind.INTERFACE
                || node.kind() == NodeKind.RECORD
                || node.kind() == NodeKind.COMPONENT
                || node.kind() == NodeKind.FUNCTION;
    }

    private int componentRank(CodeNode node) {
        if (node.kind() == NodeKind.COMPONENT) {
            return 0;
        }
        if (node.isController()) {
            return 1;
        }
        if (node.isService()) {
            return 2;
        }
        if (node.isRepository()) {
            return 3;
        }
        if (node.kind() == NodeKind.INTERFACE) {
            return 4;
        }
        if (node.kind() == NodeKind.CLASS || node.kind() == NodeKind.RECORD) {
            return 5;
        }
        return 6;
    }

    private String componentDescription(CodeNode node) {
        String path = node.filePath() == null ? "" : node.filePath().toString().replace('\\', '/');
        String suffix = path.isBlank() ? "" : " in " + path;
        return readableKind(node.kind()) + suffix;
    }

    private String componentTechnology(CodeNode node) {
        return node.language() == null || node.language().isBlank() ? "Code" : node.language();
    }

    private String readableKind(NodeKind kind) {
        String lower = kind.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String shortName(String name) {
        if (name == null || name.length() <= 34) {
            return name == null ? "Component" : name;
        }
        return name.substring(0, 31) + "...";
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

    private String tagFor(String module) {
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

    private record ComponentCandidate(String id, String name, String description, String technology) {
    }
}
