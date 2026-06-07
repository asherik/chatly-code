package com.chatlycode.language.java.extract;

import com.chatlycode.language.spi.ExtractedEdge;
import com.chatlycode.language.spi.ExtractedNode;
import com.chatlycode.language.spi.ExtractionResult;
import com.chatlycode.language.spi.LanguageExtractor;
import com.chatlycode.language.spi.SourceFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JavaSourceExtractor implements LanguageExtractor {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.*]+)\\s*;");
    private static final Pattern TYPE_PATTERN = Pattern.compile("\\b(class|interface|record|enum)\\s+([A-Za-z_$][\\w$]*)");
    private static final Pattern SPRING_ROUTE_PATTERN = Pattern.compile("@(?:GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping|RequestMapping)\\b");
    private static final Pattern SPRING_STEREOTYPE_PATTERN = Pattern.compile("@(?:Component|Service|Repository|Controller|RestController|Configuration)\\b");

    @Override
    public boolean supports(SourceFile sourceFile) {
        return sourceFile.absolutePath().getFileName().toString().endsWith(".java");
    }

    @Override
    public ExtractionResult extract(SourceFile sourceFile) {
        String packageName = packageName(sourceFile.content()).orElse("");
        List<ExtractedNode> nodes = new ArrayList<>();
        List<ExtractedEdge> edges = new ArrayList<>();
        String[] lines = sourceFile.content().split("\\R", -1);

        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            Matcher typeMatcher = TYPE_PATTERN.matcher(line);
            if (typeMatcher.find()) {
                String kind = typeMatcher.group(1);
                String simpleName = typeMatcher.group(2);
                String qualifiedName = packageName.isBlank() ? simpleName : packageName + "." + simpleName;
                String nodeId = sourceFile.relativePath() + "#" + qualifiedName;
                nodes.add(new ExtractedNode(nodeId, "java." + kind, simpleName, qualifiedName, sourceFile.relativePath(), index + 1));

                imports(sourceFile.content()).forEach(importName -> edges.add(new ExtractedEdge(nodeId, importName, "imports")));
                if (SPRING_STEREOTYPE_PATTERN.matcher(sourceFile.content()).find()) {
                    edges.add(new ExtractedEdge(nodeId, "spring.stereotype", "annotated-with"));
                }
                if (SPRING_ROUTE_PATTERN.matcher(sourceFile.content()).find()) {
                    edges.add(new ExtractedEdge(nodeId, "spring.web.route", "exposes"));
                }
            }
        }

        return new ExtractionResult(nodes, edges);
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

    private List<String> imports(String content) {
        List<String> imports = new ArrayList<>();
        for (String line : content.split("\\R")) {
            Matcher matcher = IMPORT_PATTERN.matcher(line);
            if (matcher.find()) {
                imports.add(matcher.group(1));
            }
        }
        return imports;
    }
}
