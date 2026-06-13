package com.chatlycode.problem.application;

import com.chatlycode.graph.domain.CodeEdge;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.domain.EdgeKind;
import com.chatlycode.graph.domain.NodeKind;
import com.chatlycode.problem.domain.DetectedProblem;
import com.chatlycode.problem.domain.ProblemSeverity;
import com.chatlycode.problem.domain.ProblemType;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ProblemDetector {

    private static final int HUGE_CLASS_LINE_THRESHOLD = 400;
    private static final int HUGE_METHOD_COUNT_THRESHOLD = 30;
    private static final int TOO_MANY_IMPORTS_THRESHOLD = 25;
    private static final int HIGH_BLAST_RADIUS_THRESHOLD = 12;
    private static final int LARGE_FILE_LINE_THRESHOLD = 500;
    private static final int LONG_FUNCTION_LINE_THRESHOLD = 120;
    private static final int DUMPING_GROUND_NODE_THRESHOLD = 80;
    private static final Path UNKNOWN_PATH = Path.of("");
    private Path projectRoot = Path.of("");

    public List<DetectedProblem> detect(CodeGraph graph) {
        return detect(graph, Path.of(""));
    }

    public List<DetectedProblem> detect(CodeGraph graph, Path projectRoot) {
        this.projectRoot = projectRoot == null ? Path.of("") : projectRoot;
        Map<Path, List<CodeNode>> nodesByFile = graph.nodes().stream()
                .collect(Collectors.groupingBy(CodeNode::filePath));
        Map<Path, List<CodeEdge>> edgesByFile = graph.edges().stream()
                .collect(Collectors.groupingBy(edge -> findNodePath(graph, edge.sourceId())));

        List<DetectedProblem> problems = new ArrayList<>();
        problems.addAll(detectLayerViolations(graph, nodesByFile, edgesByFile));
        problems.addAll(detectEntityLeaks(nodesByFile, edgesByFile));
        problems.addAll(detectHugeClasses(nodesByFile));
        problems.addAll(detectTooManyDependencies(nodesByFile, edgesByFile));
        problems.addAll(detectGenericNames(graph.nodes()));
        problems.addAll(detectHighBlastRadius(graph));
        problems.addAll(detectUnresolvedImports(graph));
        problems.addAll(detectDumpingGrounds(nodesByFile));
        problems.addAll(detectLongFunctions(graph.nodes()));
        problems.addAll(detectBoundaryViolations(nodesByFile, edgesByFile));
        problems.addAll(detectTextSmells(nodesByFile));
        return problems.stream()
                .sorted(Comparator.comparingInt(this::problemRank).thenComparing(problem -> normalize(problem.primaryPath())))
                .toList();
    }

    private List<DetectedProblem> detectLayerViolations(
            CodeGraph graph,
            Map<Path, List<CodeNode>> nodesByFile,
            Map<Path, List<CodeEdge>> edgesByFile
    ) {
        List<DetectedProblem> problems = new ArrayList<>();
        for (CodeNode node : graph.nodes()) {
            if (!node.isController()) {
                continue;
            }
            List<String> evidence = new ArrayList<>();
            for (CodeEdge edge : edgesByFile.getOrDefault(node.filePath(), List.of())) {
                if (!edge.sourceId().equals(node.id()) && !isEdgeFromSameType(graph, edge, node)) {
                    continue;
                }
                if ((edge.kind() == EdgeKind.TYPE_OF || edge.kind() == EdgeKind.IMPORTS || edge.kind() == EdgeKind.REFERENCES)
                        && edge.targetId().contains("Repository")) {
                    evidence.add(edge.kind().name().toLowerCase() + ": " + edge.targetId());
                }
            }
            if (!evidence.isEmpty()) {
                problems.add(new DetectedProblem(
                        "layer-violation-" + node.id().hashCode(),
                        ProblemType.LAYER_VIOLATION,
                        ProblemSeverity.ERROR,
                        0.92,
                        "Controller uses repository directly",
                        "Move data access from the controller into a service layer.",
                        evidence,
                        node.filePath(),
                        node.startLine()
                ));
            }
        }
        return problems;
    }

    private boolean isEdgeFromSameType(CodeGraph graph, CodeEdge edge, CodeNode typeNode) {
        return graph.nodes().stream()
                .anyMatch(node -> node.id().equals(edge.sourceId()) && node.filePath().equals(typeNode.filePath()));
    }

    private List<DetectedProblem> detectEntityLeaks(
            Map<Path, List<CodeNode>> nodesByFile,
            Map<Path, List<CodeEdge>> edgesByFile
    ) {
        List<DetectedProblem> problems = new ArrayList<>();
        for (Map.Entry<Path, List<CodeNode>> entry : nodesByFile.entrySet()) {
            boolean controller = entry.getValue().stream().anyMatch(CodeNode::isController);
            if (!controller) {
                continue;
            }
            List<String> evidence = edgesByFile.getOrDefault(entry.getKey(), List.of()).stream()
                    .filter(edge -> edge.kind() == EdgeKind.IMPORTS)
                    .filter(edge -> edge.targetId().contains(".entity.") || edge.targetId().endsWith("Entity"))
                    .map(edge -> "Imports entity type: " + edge.targetId())
                    .toList();
            if (!evidence.isEmpty()) {
                CodeNode controllerNode = entry.getValue().stream()
                        .filter(CodeNode::isController)
                        .findFirst()
                        .orElse(entry.getValue().getFirst());
                problems.add(new DetectedProblem(
                        "entity-leak-" + controllerNode.id().hashCode(),
                        ProblemType.ENTITY_LEAK,
                        ProblemSeverity.WARNING,
                        0.85,
                        "Entity leaks into controller/API layer",
                        "Expose DTOs or view models instead of persistence entities in the controller.",
                        evidence,
                        controllerNode.filePath(),
                        controllerNode.startLine()
                ));
            }
        }
        return problems;
    }

    private List<DetectedProblem> detectHugeClasses(Map<Path, List<CodeNode>> nodesByFile) {
        List<DetectedProblem> problems = new ArrayList<>();
        for (Map.Entry<Path, List<CodeNode>> entry : nodesByFile.entrySet()) {
            long methodCount = entry.getValue().stream().filter(node -> node.kind() == NodeKind.METHOD).count();
            int estimatedLines = entry.getValue().stream().mapToInt(CodeNode::endLine).max().orElse(0);
            if (estimatedLines < HUGE_CLASS_LINE_THRESHOLD && methodCount < HUGE_METHOD_COUNT_THRESHOLD) {
                continue;
            }
            CodeNode primary = entry.getValue().stream()
                    .filter(node -> node.kind() == NodeKind.CLASS || node.kind() == NodeKind.INTERFACE || node.kind() == NodeKind.RECORD)
                    .findFirst()
                    .orElse(entry.getValue().getFirst());
            problems.add(new DetectedProblem(
                    "huge-class-" + primary.id().hashCode(),
                    ProblemType.HUGE_CLASS,
                    ProblemSeverity.WARNING,
                    0.75,
                    "Huge class candidate",
                    "Split responsibilities into smaller cohesive types.",
                    List.of(
                            "Estimated last declaration line: " + estimatedLines,
                            "Detected methods: " + methodCount
                    ),
                    entry.getKey(),
                    primary.startLine()
            ));
        }
        return problems;
    }

    private List<DetectedProblem> detectTooManyDependencies(
            Map<Path, List<CodeNode>> nodesByFile,
            Map<Path, List<CodeEdge>> edgesByFile
    ) {
        List<DetectedProblem> problems = new ArrayList<>();
        for (Map.Entry<Path, List<CodeNode>> entry : nodesByFile.entrySet()) {
            long importCount = edgesByFile.getOrDefault(entry.getKey(), List.of()).stream()
                    .filter(edge -> edge.kind() == EdgeKind.IMPORTS)
                    .count();
            if (importCount < TOO_MANY_IMPORTS_THRESHOLD) {
                continue;
            }
            CodeNode primary = entry.getValue().getFirst();
            problems.add(new DetectedProblem(
                    "too-many-deps-" + primary.id().hashCode(),
                    ProblemType.TOO_MANY_DEPENDENCIES,
                    ProblemSeverity.INFO,
                    0.7,
                    "Too many dependencies",
                    "Review whether the type can be split or dependencies reduced.",
                    List.of("Import edges: " + importCount),
                    entry.getKey(),
                    primary.startLine()
            ));
        }
        return problems;
    }

    private List<DetectedProblem> detectGenericNames(List<CodeNode> nodes) {
        List<DetectedProblem> problems = new ArrayList<>();
        for (CodeNode node : nodes) {
            if (node.kind() != NodeKind.CLASS && node.kind() != NodeKind.INTERFACE && node.kind() != NodeKind.MODULE) {
                continue;
            }
            String name = node.name();
            String lower = name.toLowerCase();
            if (name.endsWith("Manager")) {
                problems.add(genericNameProblem(node, ProblemSeverity.WARNING, "Generic manager name"));
            } else if (name.endsWith("Util") || name.endsWith("Utils") || lower.equals("utils") || lower.equals("helpers")) {
                problems.add(genericNameProblem(node, ProblemSeverity.INFO, "Utility class name"));
            } else if (lower.equals("common") || lower.equals("shared") || lower.equals("types")) {
                problems.add(genericNameProblem(node, ProblemSeverity.INFO, "Broad module name"));
            }
        }
        return problems;
    }

    private DetectedProblem genericNameProblem(CodeNode node, ProblemSeverity severity, String title) {
        return new DetectedProblem(
                "generic-name-" + node.id().hashCode(),
                ProblemType.GENERIC_NAME,
                severity,
                0.6,
                title,
                "Prefer a domain-specific name that explains responsibility.",
                List.of("Type: " + node.qualifiedName()),
                node.filePath(),
                node.startLine()
        );
    }

    private List<DetectedProblem> detectHighBlastRadius(CodeGraph graph) {
        Map<String, Set<String>> incoming = new HashMap<>();
        for (CodeEdge edge : graph.edges()) {
            if (edge.kind() != EdgeKind.IMPORTS && edge.kind() != EdgeKind.REFERENCES && edge.kind() != EdgeKind.CALLS) {
                continue;
            }
            incoming.computeIfAbsent(edge.targetId(), ignored -> new HashSet<>()).add(edge.sourceId());
        }
        List<DetectedProblem> problems = new ArrayList<>();
        for (CodeNode node : graph.nodes()) {
            int inbound = incoming.getOrDefault(node.qualifiedName(), Set.of()).size()
                    + incoming.getOrDefault(node.id(), Set.of()).size();
            if (inbound < HIGH_BLAST_RADIUS_THRESHOLD) {
                continue;
            }
            problems.add(new DetectedProblem(
                    "blast-radius-" + node.id().hashCode(),
                    ProblemType.HIGH_BLAST_RADIUS,
                    ProblemSeverity.WARNING,
                    0.8,
                    "High blast-radius file",
                    "Changes here may affect many dependents. Add tests and review impact radius before editing.",
                    List.of("Inbound references: " + inbound),
                    node.filePath(),
                    node.startLine()
            ));
        }
        return problems;
    }

    private List<DetectedProblem> detectUnresolvedImports(CodeGraph graph) {
        Set<String> nodeIds = graph.nodes().stream().map(CodeNode::id).collect(Collectors.toSet());
        Map<Path, List<CodeEdge>> unresolvedByFile = graph.edges().stream()
                .filter(edge -> edge.kind() == EdgeKind.IMPORTS)
                .filter(edge -> !nodeIds.contains(edge.targetId()) && looksProjectLocal(edge.targetId()))
                .collect(Collectors.groupingBy(edge -> findNodePath(graph, edge.sourceId()), LinkedHashMap::new, Collectors.toList()));
        List<DetectedProblem> problems = new ArrayList<>();
        for (Map.Entry<Path, List<CodeEdge>> entry : unresolvedByFile.entrySet()) {
            if (entry.getKey().equals(UNKNOWN_PATH) || entry.getValue().size() < 3) {
                continue;
            }
            problems.add(new DetectedProblem(
                    "unresolved-import-" + entry.getKey().hashCode(),
                    ProblemType.UNRESOLVED_IMPORT,
                    ProblemSeverity.INFO,
                    0.55,
                    "Unresolved local imports",
                    "Imports in this file could not be linked to graph nodes. Check aliases, generated code, or missing extractor support.",
                    entry.getValue().stream().map(edge -> edge.targetId()).distinct().limit(8).toList(),
                    entry.getKey(),
                    firstLine(entry.getValue())
            ));
        }
        return problems;
    }

    private List<DetectedProblem> detectDumpingGrounds(Map<Path, List<CodeNode>> nodesByFile) {
        List<DetectedProblem> problems = new ArrayList<>();
        for (Map.Entry<Path, List<CodeNode>> entry : nodesByFile.entrySet()) {
            String path = normalize(entry.getKey());
            if (!isDumpingGroundPath(path) || entry.getValue().size() < DUMPING_GROUND_NODE_THRESHOLD) {
                continue;
            }
            CodeNode primary = entry.getValue().getFirst();
            problems.add(new DetectedProblem(
                    "dumping-ground-" + entry.getKey().hashCode(),
                    ProblemType.DUMPING_GROUND,
                    ProblemSeverity.WARNING,
                    0.72,
                    "Dumping-ground module",
                    "This broad module contains many declarations. Split by domain concept or feature boundary.",
                    List.of("Path: " + path, "Graph nodes in file: " + entry.getValue().size()),
                    entry.getKey(),
                    primary.startLine()
            ));
        }
        return problems;
    }

    private List<DetectedProblem> detectLongFunctions(List<CodeNode> nodes) {
        return nodes.stream()
                .filter(node -> node.kind() == NodeKind.FUNCTION || node.kind() == NodeKind.METHOD)
                .filter(node -> node.endLine() - node.startLine() + 1 >= LONG_FUNCTION_LINE_THRESHOLD)
                .map(node -> new DetectedProblem(
                        "long-function-" + node.id().hashCode(),
                        ProblemType.LONG_FUNCTION,
                        ProblemSeverity.WARNING,
                        0.74,
                        "Long function candidate",
                        "Split the function into smaller steps with one clear responsibility.",
                        List.of("Function: " + node.qualifiedName(), "Lines: " + (node.endLine() - node.startLine() + 1)),
                        node.filePath(),
                        node.startLine()
                ))
                .limit(80)
                .toList();
    }

    private List<DetectedProblem> detectBoundaryViolations(
            Map<Path, List<CodeNode>> nodesByFile,
            Map<Path, List<CodeEdge>> edgesByFile
    ) {
        List<DetectedProblem> problems = new ArrayList<>();
        for (Map.Entry<Path, List<CodeNode>> entry : nodesByFile.entrySet()) {
            String sourcePath = normalize(entry.getKey());
            List<String> evidence = new ArrayList<>();
            for (CodeEdge edge : edgesByFile.getOrDefault(entry.getKey(), List.of())) {
                String target = normalize(edge.targetId());
                if (isUiPath(sourcePath) && mentionsStorageOrCoreInternal(target)) {
                    evidence.add(edge.kind() + ": " + edge.targetId());
                } else if (isCorePath(sourcePath) && mentionsUiOrDesktop(target)) {
                    evidence.add(edge.kind() + ": " + edge.targetId());
                }
            }
            if (!evidence.isEmpty()) {
                CodeNode primary = entry.getValue().getFirst();
                problems.add(new DetectedProblem(
                        "boundary-violation-" + entry.getKey().hashCode(),
                        ProblemType.BOUNDARY_VIOLATION,
                        ProblemSeverity.WARNING,
                        0.68,
                        "Suspicious cross-boundary dependency",
                        "Keep UI, core/domain, storage, and desktop integration dependencies pointed in one direction.",
                        evidence.stream().distinct().limit(8).toList(),
                        entry.getKey(),
                        primary.startLine()
                ));
            }
        }
        return problems;
    }

    private List<DetectedProblem> detectTextSmells(Map<Path, List<CodeNode>> nodesByFile) {
        List<DetectedProblem> problems = new ArrayList<>();
        for (Path path : nodesByFile.keySet().stream().sorted(Comparator.comparing(this::normalize)).toList()) {
            String normalizedPath = normalize(path);
            if (!isSourcePath(normalizedPath)) {
                continue;
            }
            List<String> lines = readLines(path);
            if (lines.isEmpty()) {
                continue;
            }
            if (lines.size() >= LARGE_FILE_LINE_THRESHOLD) {
                problems.add(textProblem(path, ProblemType.LARGE_FILE, ProblemSeverity.WARNING, 0.7,
                        "Large source file", "Large files usually hide multiple responsibilities.",
                        List.of("Lines: " + lines.size()), 1));
            }
            addPatternProblem(problems, path, lines, ProblemType.DEBUG_ARTIFACT, ProblemSeverity.WARNING, 0.78,
                    "Debug artifact in production code", "Remove debug macros, console logs, or print statements from production paths.",
                    List.of("dbg!(", "println!(", "eprintln!(", "console.log(", "console.warn("), true);
            addPatternProblem(problems, path, lines, ProblemType.TODO_MARKER, ProblemSeverity.INFO, 0.62,
                    "TODO/FIXME marker", "Track or resolve TODO/FIXME markers before release.",
                    List.of("TODO", "FIXME", "HACK"), false);
            addPatternProblem(problems, path, lines, ProblemType.PANIC_USAGE, ProblemSeverity.WARNING, 0.76,
                    "Panic-style failure path", "Prefer typed errors or explicit recovery in production code.",
                    List.of("panic!(", "todo!(", "unimplemented!(", ".unwrap()", ".expect("), true);
            addPatternProblem(problems, path, lines, ProblemType.SECRET_EXPOSURE, ProblemSeverity.ERROR, 0.82,
                    "Possible hardcoded secret", "Move secrets to environment variables or a secret store.",
                    List.of("api_key", "apikey", "secret", "password", "token"), true);
            addPatternProblem(problems, path, lines, ProblemType.HARDCODED_ENDPOINT, ProblemSeverity.INFO, 0.64,
                    "Hardcoded endpoint", "Put environment-specific URLs and ports behind configuration.",
                    List.of("http://", "https://", "localhost:", "127.0.0.1:"), false);
            if (normalizedPath.endsWith(".rs")) {
                addPatternProblem(problems, path, lines, ProblemType.ASYNC_LOCK_HOTSPOT, ProblemSeverity.WARNING, 0.7,
                        "Locking hotspot candidate", "Review lock scope and avoid blocking or long-held locks in async/runtime paths.",
                        List.of("Arc<Mutex", "Mutex<", "RwLock<", ".lock().await", ".blocking_lock()"), false);
            }
        }
        return problems;
    }

    private DetectedProblem textProblem(Path path, ProblemType type, ProblemSeverity severity, double confidence,
                                        String title, String description, List<String> evidence, int line) {
        return new DetectedProblem(type.name().toLowerCase() + "-" + path.hashCode(), type, severity, confidence,
                title, description, evidence, path, line);
    }

    private void addPatternProblem(List<DetectedProblem> problems, Path path, List<String> lines, ProblemType type,
                                   ProblemSeverity severity, double confidence, String title, String description,
                                   List<String> patterns, boolean skipTests) {
        String normalizedPath = normalize(path);
        if (skipTests && isTestPath(normalizedPath)) {
            return;
        }
        List<String> evidence = new ArrayList<>();
        int firstLine = 1;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String lower = line.toLowerCase();
            boolean matched = patterns.stream().anyMatch(pattern -> lower.contains(pattern.toLowerCase()));
            if (!matched || isLikelyCommentOnly(line) || isGeneratedLikeLine(line)) {
                continue;
            }
            if (type == ProblemType.SECRET_EXPOSURE && !looksLikeAssignedSecret(line)) {
                continue;
            }
            if (type == ProblemType.PANIC_USAGE && looksLikeTestAssertion(line)) {
                continue;
            }
            if (evidence.isEmpty()) {
                firstLine = index + 1;
            }
            evidence.add("line " + (index + 1) + ": " + sanitizeSnippet(line));
            if (evidence.size() >= 5) {
                break;
            }
        }
        if (!evidence.isEmpty()) {
            problems.add(textProblem(path, type, severity, confidence, title, description, evidence, firstLine));
        }
    }

    private List<String> readLines(Path path) {
        try {
            Path resolved = resolvePath(path);
            if (resolved == null || Files.notExists(resolved) || Files.size(resolved) > 256_000) {
                return List.of();
            }
            return Files.readAllLines(resolved);
        } catch (IOException | RuntimeException ignored) {
            return List.of();
        }
    }

    private Path resolvePath(Path path) {
        if (path == null) {
            return null;
        }
        if (path.isAbsolute() || projectRoot.toString().isBlank()) {
            return path;
        }
        return projectRoot.resolve(path).normalize();
    }

    private boolean looksLikeAssignedSecret(String line) {
        String lower = line.toLowerCase();
        if (lower.contains("env::var") || lower.contains("process.env") || lower.contains("searchparams")
                || lower.contains("passwordinput") || lower.contains("showpassword") || lower.contains("htmlfor=\"password\"")
                || lower.contains("type=\"password\"") || lower.contains("passwordhash::new")
                || lower.contains("password_hash") || lower.contains("password_hasher") || lower.contains("requires ")
                || lower.contains("href=") || lower.contains("classname=") || lower.contains("text:")
                || lower.contains("copied to clipboard") || lower.contains("deleted") || lower.contains("saved")) {
            return false;
        }
        boolean assigned = lower.contains("=") || lower.contains(":");
        boolean hasLiteral = line.matches(".*[=:]\\s*[\"'][^\"']{8,}[\"'].*");
        boolean suspiciousName = lower.contains("api_key") || lower.contains("apikey") || lower.contains("secret")
                || lower.contains("access_key") || lower.contains("private_key") || lower.contains("bot_token")
                || lower.contains("auth_token");
        boolean placeholder = lower.contains("your_") || lower.contains("example") || lower.contains("changeme")
                || lower.contains("placeholder") || lower.contains("dummy");
        boolean looksLikeSecretValue = line.matches(".*[\"'](sk-[A-Za-z0-9_-]{12,}|[A-Za-z0-9_/-]*(secret|token|key)[A-Za-z0-9_/-]{8,}|[A-Za-z0-9+/=]{32,})[\"'].*");
        return assigned && hasLiteral && suspiciousName && looksLikeSecretValue && !placeholder;
    }

    private boolean looksProjectLocal(String target) {
        String lower = target.toLowerCase();
        return lower.startsWith("crate::") || lower.startsWith("super::") || lower.startsWith("self::")
                || lower.startsWith("./") || lower.startsWith("../") || lower.startsWith("@/")
                || lower.contains("chatly") || lower.contains("core") || lower.contains("shared");
    }

    private boolean isDumpingGroundPath(String path) {
        return path.endsWith("/types.ts") || path.endsWith("/types.rs") || path.endsWith("/utils.ts")
                || path.endsWith("/utils.rs") || path.endsWith("/helpers.ts") || path.endsWith("/common.ts")
                || path.contains("/shared/") || path.contains("/src/types/");
    }

    private boolean isSourcePath(String path) {
        return path.endsWith(".java") || path.endsWith(".rs") || path.endsWith(".ts") || path.endsWith(".tsx")
                || path.endsWith(".js") || path.endsWith(".jsx");
    }

    private boolean isTestPath(String path) {
        return path.contains("/test/") || path.contains("/tests/") || path.endsWith("_test.rs")
                || path.endsWith(".test.ts") || path.endsWith(".spec.ts") || path.endsWith(".test.tsx")
                || path.endsWith(".spec.tsx");
    }

    private boolean isUiPath(String path) {
        return path.contains("/ui/") || path.contains("/components/") || path.contains("/pages/")
                || path.contains("packages/ui/") || path.endsWith(".tsx") || path.endsWith(".jsx");
    }

    private boolean isCorePath(String path) {
        return path.startsWith("core/") || path.contains("/core/src/") || path.contains("chatly_core");
    }

    private boolean mentionsStorageOrCoreInternal(String target) {
        return target.contains("storage") || target.contains("database") || target.contains("repository")
                || target.contains("chatly_core::storage") || target.contains("core/src/storage");
    }

    private boolean mentionsUiOrDesktop(String target) {
        return target.contains("packages/ui") || target.contains("components") || target.contains("tauri")
                || target.contains("desktop") || target.contains("webview");
    }

    private boolean isLikelyCommentOnly(String line) {
        String trimmed = line.stripLeading();
        return trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*");
    }

    private boolean isGeneratedLikeLine(String line) {
        return line.length() > 500;
    }

    private boolean looksLikeTestAssertion(String line) {
        String lower = line.toLowerCase();
        return lower.contains("assert_") || lower.contains("assert!") || lower.contains("expected ")
                || lower.contains("never fails") || lower.contains("#[ignore");
    }

    private String sanitizeSnippet(String line) {
        String trimmed = line.trim().replaceAll("\\s+", " ");
        return trimmed.length() <= 160 ? trimmed : trimmed.substring(0, 157) + "...";
    }

    private int firstLine(List<CodeEdge> edges) {
        return edges.stream().mapToInt(CodeEdge::line).filter(line -> line > 0).min().orElse(1);
    }

    private Path findNodePath(CodeGraph graph, String nodeId) {
        return graph.nodes().stream()
                .filter(node -> node.id().equals(nodeId))
                .map(CodeNode::filePath)
                .findFirst()
                .orElse(UNKNOWN_PATH);
    }

    private String normalize(Path path) {
        return path == null ? "" : path.toString().replace('\\', '/');
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace('\\', '/').toLowerCase();
    }

    private int problemRank(DetectedProblem problem) {
        int severity = switch (problem.severity()) {
            case ERROR -> 0;
            case WARNING -> 10;
            case INFO -> 20;
        };
        int type = switch (problem.type()) {
            case SECRET_EXPOSURE -> 0;
            case PANIC_USAGE, DEBUG_ARTIFACT -> 1;
            case BOUNDARY_VIOLATION, CIRCULAR_DEPENDENCY, LAYER_VIOLATION -> 2;
            case ASYNC_LOCK_HOTSPOT, HIGH_BLAST_RADIUS -> 3;
            case DUMPING_GROUND, LARGE_FILE, LONG_FUNCTION -> 4;
            case TOO_MANY_DEPENDENCIES, UNRESOLVED_IMPORT -> 5;
            case TODO_MARKER, GENERIC_NAME -> 6;
            default -> 7;
        };
        return severity + type;
    }
}
