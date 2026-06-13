package com.chatlycode.language.java.extract;

import com.chatlycode.language.spi.ExtractedEdge;
import com.chatlycode.language.spi.ExtractedNode;
import com.chatlycode.language.spi.ExtractionResult;
import com.chatlycode.language.spi.LanguageExtractor;
import com.chatlycode.language.spi.SourceFile;
import com.chatlycode.language.spi.UnresolvedReference;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JavaSourceExtractor implements LanguageExtractor {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.*]+)\\s*;");
    private static final Pattern TYPE_PATTERN = Pattern.compile("\\b(class|interface|record|enum)\\s+([A-Za-z_$][\\w$]*)");
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile("@([A-Za-z_$][\\w$]*)");
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "(?:private|protected|public)\\s+([\\w.<>, ?]+?)\\s+([A-Za-z_$][\\w$]*)\\s*(?:=\\s*[^;]+)?;"
    );
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(?:public|protected|private)\\s+[\\w.<>, \\[\\]]+\\s+([A-Za-z_$][\\w$]*)\\s*\\([^;]*\\)\\s*(?:throws\\s+[\\w., ]+)?\\s*\\{?"
    );
    private static final Pattern MAPPING_PATTERN = Pattern.compile(
            "@(?:GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping|RequestMapping)\\s*\\(([^)]*)\\)"
    );

    @Override
    public boolean supports(SourceFile sourceFile) {
        return sourceFile.absolutePath().getFileName().toString().endsWith(".java");
    }

    @Override
    public ExtractionResult extract(SourceFile sourceFile) {
        Instant started = Instant.now();
        String packageName = packageName(sourceFile.content()).orElse("");
        List<ExtractedNode> nodes = new ArrayList<>();
        List<ExtractedEdge> edges = new ArrayList<>();
        List<UnresolvedReference> unresolved = new ArrayList<>();
        String[] lines = sourceFile.content().split("\\R", -1);
        List<String> pendingAnnotations = new ArrayList<>();
        List<PendingImport> pendingImports = new ArrayList<>();
        String currentTypeId = null;

        if (!packageName.isBlank()) {
            String packageId = stableId(sourceFile, "package", packageName, 1);
            nodes.add(new ExtractedNode(
                    packageId,
                    "package",
                    packageName,
                    packageName,
                    sourceFile.relativePath(),
                    1,
                    1,
                    "",
                    List.of()
            ));
        }

        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            int lineNumber = index + 1;
            collectAnnotations(line, pendingAnnotations);

            Matcher importMatcher = IMPORT_PATTERN.matcher(line);
            if (importMatcher.find()) {
                String importName = importMatcher.group(1);
                String importId = stableId(sourceFile, "import", importName, lineNumber);
                nodes.add(new ExtractedNode(
                        importId,
                        "import",
                        importName,
                        importName,
                        sourceFile.relativePath(),
                        lineNumber,
                        lineNumber,
                        "",
                        List.of()
                ));
                pendingImports.add(new PendingImport(importId, importName, lineNumber));
                continue;
            }

            Matcher typeMatcher = TYPE_PATTERN.matcher(line);
            if (typeMatcher.find()) {
                String kind = mapTypeKind(typeMatcher.group(1));
                String simpleName = typeMatcher.group(2);
                String qualifiedName = packageName.isBlank() ? simpleName : packageName + "." + simpleName;
                currentTypeId = stableId(sourceFile, kind, qualifiedName, lineNumber);
                List<String> decorators = List.copyOf(pendingAnnotations);
                nodes.add(new ExtractedNode(
                        currentTypeId,
                        kind,
                        simpleName,
                        qualifiedName,
                        sourceFile.relativePath(),
                        lineNumber,
                        lineNumber,
                        line.trim(),
                        decorators
                ));
                if (!packageName.isBlank()) {
                    String packageId = stableId(sourceFile, "package", packageName, 1);
                    edges.add(edge(packageId, currentTypeId, "contains", "tree_sitter", 1.0, lineNumber));
                }
                for (PendingImport pendingImport : pendingImports) {
                    edges.add(edge(currentTypeId, pendingImport.name(), "imports", "tree_sitter", 0.7, pendingImport.line()));
                    edges.add(edge(currentTypeId, pendingImport.id(), "contains", "tree_sitter", 1.0, pendingImport.line()));
                }
                for (String decorator : decorators) {
                    edges.add(edge(currentTypeId, decorator, "decorates", "framework_resolver", 0.95, lineNumber));
                }
                addRouteNodes(sourceFile, nodes, edges, currentTypeId, line, lineNumber);
                pendingAnnotations.clear();
                continue;
            }

            Matcher fieldMatcher = FIELD_PATTERN.matcher(line);
            if (fieldMatcher.find() && currentTypeId != null) {
                String fieldType = fieldMatcher.group(1).trim();
                String fieldName = fieldMatcher.group(2);
                String fieldId = stableId(sourceFile, "field", currentTypeId + "#" + fieldName, lineNumber);
                nodes.add(new ExtractedNode(
                        fieldId,
                        "field",
                        fieldName,
                        fieldType,
                        sourceFile.relativePath(),
                        lineNumber,
                        lineNumber,
                        line.trim(),
                        List.of()
                ));
                edges.add(edge(currentTypeId, fieldId, "contains", "tree_sitter", 1.0, lineNumber));
                edges.add(edge(fieldId, fieldType, "type_of", "heuristic", 0.7, lineNumber));
                unresolved.add(new UnresolvedReference(fieldId, fieldType, "type_of", sourceFile.relativePath(), lineNumber, List.of()));
                continue;
            }

            Matcher methodMatcher = METHOD_PATTERN.matcher(line);
            if (methodMatcher.find() && currentTypeId != null && !line.contains(" class ")) {
                String methodName = methodMatcher.group(1);
                if (!isKeyword(methodName)) {
                    String methodId = stableId(sourceFile, "method", currentTypeId + "#" + methodName, lineNumber);
                    nodes.add(new ExtractedNode(
                            methodId,
                            "method",
                            methodName,
                            methodName,
                            sourceFile.relativePath(),
                            lineNumber,
                            lineNumber,
                            line.trim(),
                            List.of()
                    ));
                    edges.add(edge(currentTypeId, methodId, "contains", "tree_sitter", 1.0, lineNumber));
                }
            }
        }

        Duration duration = Duration.between(started, Instant.now());
        return new ExtractionResult(nodes, edges, unresolved, List.of(), duration);
    }

    private void addRouteNodes(
            SourceFile sourceFile,
            List<ExtractedNode> nodes,
            List<ExtractedEdge> edges,
            String ownerTypeId,
            String line,
            int lineNumber
    ) {
        Matcher mappingMatcher = MAPPING_PATTERN.matcher(line);
        if (!mappingMatcher.find()) {
            return;
        }
        String routeValue = mappingMatcher.group(1).replace("\"", "").trim();
        if (routeValue.isBlank()) {
            routeValue = "/";
        }
        String routeId = stableId(sourceFile, "route", routeValue, lineNumber);
        nodes.add(new ExtractedNode(
                routeId,
                "route",
                routeValue,
                routeValue,
                sourceFile.relativePath(),
                lineNumber,
                lineNumber,
                line.trim(),
                List.of()
        ));
        edges.add(edge(ownerTypeId, routeId, "handles_route", "framework_resolver", 0.9, lineNumber));
    }

    private ExtractedEdge edge(String sourceId, String targetId, String kind, String provenance, double confidence, int line) {
        return new ExtractedEdge(sourceId, targetId, kind, provenance, confidence, line, "");
    }

    private String stableId(SourceFile sourceFile, String kind, String name, int line) {
        return sourceFile.relativePath() + "#" + kind + ":" + name + ":" + line;
    }

    private String mapTypeKind(String rawKind) {
        return switch (rawKind) {
            case "interface" -> "interface";
            case "record" -> "record";
            case "enum" -> "enum";
            default -> "class";
        };
    }

    private void collectAnnotations(String line, List<String> pendingAnnotations) {
        Matcher matcher = ANNOTATION_PATTERN.matcher(line);
        while (matcher.find()) {
            pendingAnnotations.add(matcher.group(1));
        }
    }

    private boolean isKeyword(String name) {
        return switch (name) {
            case "if", "for", "while", "switch", "catch", "return", "new", "class" -> true;
            default -> false;
        };
    }

    private Optional<String> packageName(String content) {
        for (String line : content.split("\\R")) {
            Matcher matcher = PACKAGE_PATTERN.matcher(line);
            if (matcher.find()) {
                return Optional.of(matcher.group(1));
            }
        }
        return Optional.empty();
    }

    private record PendingImport(String id, String name, int line) {
    }
}
