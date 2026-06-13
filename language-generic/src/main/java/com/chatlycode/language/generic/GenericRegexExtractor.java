package com.chatlycode.language.generic;

import com.chatlycode.language.spi.ExtractedEdge;
import com.chatlycode.language.spi.ExtractedNode;
import com.chatlycode.language.spi.ExtractionResult;
import com.chatlycode.language.spi.LanguageExtractor;
import com.chatlycode.language.spi.SourceFile;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

final class GenericRegexExtractor implements LanguageExtractor {

    private final List<String> supportedExtensions;
    private final GenericPatternSet patterns;

    GenericRegexExtractor(List<String> supportedExtensions, GenericPatternSet patterns) {
        this.supportedExtensions = List.copyOf(supportedExtensions);
        this.patterns = patterns;
    }

    @Override
    public boolean supports(SourceFile sourceFile) {
        String fileName = sourceFile.absolutePath().getFileName().toString();
        return supportedExtensions.stream().anyMatch(fileName::endsWith);
    }

    @Override
    public ExtractionResult extract(SourceFile sourceFile) {
        Instant started = Instant.now();
        List<ExtractedNode> nodes = new ArrayList<>();
        List<ExtractedEdge> edges = new ArrayList<>();
        String moduleId = moduleId(sourceFile);

        nodes.add(node(sourceFile, moduleId, "module", moduleName(sourceFile), moduleName(sourceFile), 1, ""));

        String[] lines = sourceFile.content().split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            int lineNumber = index + 1;
            addImport(sourceFile, nodes, edges, moduleId, line, lineNumber);
            addModule(sourceFile, nodes, edges, moduleId, line, lineNumber);
            addDeclaredNode(sourceFile, nodes, edges, moduleId, line, lineNumber);
        }

        return new ExtractionResult(nodes, edges, List.of(), List.of(), Duration.between(started, Instant.now()));
    }

    private void addImport(
            SourceFile sourceFile,
            List<ExtractedNode> nodes,
            List<ExtractedEdge> edges,
            String moduleId,
            String line,
            int lineNumber
    ) {
        Matcher matcher = patterns.importPattern().matcher(line);
        if (!matcher.find()) {
            return;
        }
        String name = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
        if (name == null || name.isBlank()) {
            return;
        }
        String id = stableId(sourceFile, "import", name, lineNumber);
        nodes.add(node(sourceFile, id, "import", name, name, lineNumber, line.trim()));
        edges.add(edge(moduleId, id, "imports", lineNumber));
    }

    private void addModule(
            SourceFile sourceFile,
            List<ExtractedNode> nodes,
            List<ExtractedEdge> edges,
            String parentModuleId,
            String line,
            int lineNumber
    ) {
        Matcher matcher = patterns.modulePattern().matcher(line);
        if (!matcher.find()) {
            return;
        }
        String name = matcher.group(1);
        String id = stableId(sourceFile, "module", name, lineNumber);
        nodes.add(node(sourceFile, id, "module", name, qualify(sourceFile, name), lineNumber, line.trim()));
        edges.add(edge(parentModuleId, id, "contains", lineNumber));
    }

    private void addDeclaredNode(
            SourceFile sourceFile,
            List<ExtractedNode> nodes,
            List<ExtractedEdge> edges,
            String moduleId,
            String line,
            int lineNumber
    ) {
        for (GenericPatternSet.NodePattern nodePattern : patterns.nodePatterns()) {
            Matcher matcher = nodePattern.pattern().matcher(line);
            if (!matcher.find()) {
                continue;
            }
            String name = matcher.group(1);
            String id = stableId(sourceFile, nodePattern.kind(), name, lineNumber);
            nodes.add(node(sourceFile, id, nodePattern.kind(), name, qualify(sourceFile, name), lineNumber, line.trim()));
            edges.add(edge(moduleId, id, "contains", lineNumber));
            return;
        }
    }

    private ExtractedNode node(SourceFile sourceFile, String id, String kind, String name, String qualifiedName, int line, String signature) {
        return new ExtractedNode(id, kind, name, qualifiedName, sourceFile.relativePath(), line, line, signature, List.of());
    }

    private ExtractedEdge edge(String sourceId, String targetId, String kind, int line) {
        return new ExtractedEdge(sourceId, targetId, kind, "heuristic", 0.65, line, "");
    }

    private String stableId(SourceFile sourceFile, String kind, String name, int line) {
        return sourceFile.relativePath() + "#" + kind + ":" + name + ":" + line;
    }

    private String moduleId(SourceFile sourceFile) {
        return stableId(sourceFile, "module", moduleName(sourceFile), 1);
    }

    private String moduleName(SourceFile sourceFile) {
        return sourceFile.relativePath().toString().replace('\\', '/');
    }

    private String qualify(SourceFile sourceFile, String name) {
        return moduleName(sourceFile) + "::" + name;
    }
}
