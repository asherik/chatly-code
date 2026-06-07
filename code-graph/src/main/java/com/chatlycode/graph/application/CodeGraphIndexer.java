package com.chatlycode.graph.application;

import com.chatlycode.graph.domain.CodeEdge;
import com.chatlycode.graph.domain.CodeFile;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.language.spi.ExtractionResult;
import com.chatlycode.language.spi.LanguagePlugin;
import com.chatlycode.language.spi.SourceFile;
import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.shared.time.ClockProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public final class CodeGraphIndexer {

    private final List<LanguagePlugin> languagePlugins;
    private final ClockProvider clock;

    public CodeGraphIndexer(List<LanguagePlugin> languagePlugins, ClockProvider clock) {
        this.languagePlugins = List.copyOf(languagePlugins);
        this.clock = clock;
    }

    public CodeGraph index(OpenedProject project) {
        List<CodeFile> files = new ArrayList<>();
        List<CodeNode> nodes = new ArrayList<>();
        List<CodeEdge> edges = new ArrayList<>();

        for (Path sourcePath : sourceFiles(project.root())) {
            String content = readUtf8(sourcePath);
            SourceFile sourceFile = new SourceFile(project.root(), sourcePath, content);
            languagePlugins.stream()
                    .filter(plugin -> plugin.extractor().supports(sourceFile))
                    .findFirst()
                    .ifPresent(plugin -> {
                        files.add(new CodeFile(sourceFile.relativePath(), plugin.languageId(), sha256(content)));
                        ExtractionResult result = plugin.extractor().extract(sourceFile);
                        result.nodes().forEach(node -> nodes.add(new CodeNode(
                                node.stableId(),
                                node.kind(),
                                node.name(),
                                node.qualifiedName(),
                                node.relativePath(),
                                node.line()
                        )));
                        result.edges().forEach(edge -> edges.add(new CodeEdge(edge.fromNodeId(), edge.toName(), edge.kind())));
                    });
        }

        return new CodeGraph(project.id(), files, nodes, edges, clock.now());
    }

    private List<Path> sourceFiles(Path root) {
        try (var stream = Files.walk(root, 20)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> !isIgnored(root.relativize(path)))
                    .filter(this::hasSupportedExtension)
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to walk project files: " + root, exception);
        }
    }

    private boolean hasSupportedExtension(Path path) {
        String fileName = path.getFileName().toString();
        return languagePlugins.stream()
                .flatMap(plugin -> plugin.supportedExtensions().stream())
                .anyMatch(fileName::endsWith);
    }

    private boolean isIgnored(Path relativePath) {
        String normalized = relativePath.toString().replace('\\', '/');
        return normalized.startsWith(".git/")
                || normalized.contains("/build/")
                || normalized.contains("/target/")
                || normalized.contains("/node_modules/");
    }

    private String readUtf8(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
