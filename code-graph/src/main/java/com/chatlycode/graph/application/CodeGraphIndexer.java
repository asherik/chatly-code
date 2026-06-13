package com.chatlycode.graph.application;

import com.chatlycode.graph.domain.CodeEdge;
import com.chatlycode.graph.domain.CodeFile;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.domain.CodeUnresolvedReference;
import com.chatlycode.graph.domain.EdgeProvenance;
import com.chatlycode.graph.domain.ExtractionError;
import com.chatlycode.graph.domain.IndexPhase;
import com.chatlycode.graph.domain.IndexProgress;
import com.chatlycode.graph.domain.IndexResult;
import com.chatlycode.graph.domain.NodeKind;
import com.chatlycode.graph.inventory.FileInventoryService;
import com.chatlycode.graph.inventory.InventoryFile;
import com.chatlycode.graph.mapper.GraphEdgeMapper;
import com.chatlycode.graph.mapper.GraphNodeMapper;
import com.chatlycode.graph.resolution.ReferenceResolver;
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class CodeGraphIndexer {

    private static final int FILE_IO_BATCH_SIZE = 10;

    private final List<LanguagePlugin> languagePlugins;
    private final ClockProvider clock;
    private final FileInventoryService fileInventoryService;
    private final GraphNodeMapper nodeMapper = new GraphNodeMapper();
    private final GraphEdgeMapper edgeMapper = new GraphEdgeMapper();
    private final ReferenceResolver referenceResolver = new ReferenceResolver();

    public CodeGraphIndexer(List<LanguagePlugin> languagePlugins, ClockProvider clock) {
        this.languagePlugins = List.copyOf(languagePlugins);
        this.clock = clock;
        this.fileInventoryService = new FileInventoryService(collectExtensions(languagePlugins));
    }

    public CodeGraph index(OpenedProject project) {
        return index(project, ignored -> {});
    }

    public CodeGraph index(OpenedProject project, Consumer<IndexProgress> progressConsumer) {
        Instant startedAt = clock.now();
        long startedNanos = System.nanoTime();

        progressConsumer.accept(new IndexProgress(IndexPhase.SCANNING, 0, 0, null));
        List<InventoryFile> inventoryFiles = fileInventoryService.scan(project.root());

        List<CodeFile> files = new ArrayList<>();
        List<CodeNode> nodes = new ArrayList<>();
        List<CodeEdge> edges = new ArrayList<>();
        List<com.chatlycode.language.spi.UnresolvedReference> unresolvedReferences = new ArrayList<>();
        List<ExtractionError> errors = new ArrayList<>();

        int filesIndexed = 0;
        int filesSkipped = 0;
        int filesErrored = 0;
        Map<String, String> previousHashes = new LinkedHashMap<>();

        int total = inventoryFiles.size();
        for (int offset = 0; offset < inventoryFiles.size(); offset += FILE_IO_BATCH_SIZE) {
            List<InventoryFile> batch = inventoryFiles.subList(offset, Math.min(offset + FILE_IO_BATCH_SIZE, inventoryFiles.size()));
            for (InventoryFile inventoryFile : batch) {
                int current = offset + batch.indexOf(inventoryFile) + 1;
                progressConsumer.accept(new IndexProgress(IndexPhase.PARSING, current, total, inventoryFile.relativePath().toString()));

                Path absolutePath = project.root().resolve(inventoryFile.relativePath());
                String content;
                try {
                    content = Files.readString(absolutePath, StandardCharsets.UTF_8);
                } catch (IOException exception) {
                    filesErrored++;
                    errors.add(new ExtractionError(
                            "Failed to read file: " + exception.getMessage(),
                            inventoryFile.relativePath(),
                            "error",
                            "read_error"
                    ));
                    continue;
                }

                String contentHash = sha256(content);
                if (contentHash.equals(previousHashes.get(inventoryFile.relativePath().toString()))) {
                    filesSkipped++;
                    continue;
                }
                previousHashes.put(inventoryFile.relativePath().toString(), contentHash);

                LanguagePlugin plugin = findPlugin(project.root(), absolutePath, content).orElse(null);
                if (plugin == null) {
                    filesSkipped++;
                    continue;
                }

                SourceFile sourceFile = new SourceFile(project.root(), absolutePath, content);
                ExtractionResult extraction = plugin.extractor().extract(sourceFile);
                if (!extraction.errors().isEmpty()) {
                    extraction.errors().forEach(error -> errors.add(new ExtractionError(
                            error.message(),
                            error.filePath() == null ? inventoryFile.relativePath() : error.filePath(),
                            error.severity(),
                            error.code()
                    )));
                }

                Instant indexedAt = clock.now();
                CodeNode fileNode = nodeMapper.fileNode(inventoryFile.relativePath(), plugin.languageId(), indexedAt);
                nodes.add(fileNode);

                java.util.Map<String, String> stableIds = new LinkedHashMap<>();
                List<CodeNode> mappedNodes = new ArrayList<>();
                for (var extractedNode : extraction.nodes()) {
                    CodeNode mapped = nodeMapper.toCodeNode(extractedNode, plugin.languageId(), indexedAt);
                    stableIds.put(extractedNode.stableId(), mapped.id());
                    mappedNodes.add(mapped);
                    nodes.add(mapped);
                }
                for (CodeNode mapped : mappedNodes) {
                    if (mapped.kind() == NodeKind.PACKAGE || mapped.kind() == NodeKind.IMPORT) {
                        continue;
                    }
                    edges.add(edgeMapper.contains(fileNode.id(), mapped.id(), EdgeProvenance.TREE_SITTER));
                }

                for (var extractedEdge : extraction.edges()) {
                    String sourceId = stableIds.getOrDefault(extractedEdge.sourceId(), extractedEdge.sourceId());
                    String targetId = stableIds.getOrDefault(extractedEdge.targetId(), extractedEdge.targetId());
                    edges.add(edgeMapper.toCodeEdge(new com.chatlycode.language.spi.ExtractedEdge(
                            sourceId,
                            targetId,
                            extractedEdge.kind(),
                            extractedEdge.provenance(),
                            extractedEdge.confidence(),
                            extractedEdge.line(),
                            extractedEdge.metadataJson()
                    )));
                }
                unresolvedReferences.addAll(extraction.unresolvedReferences());

                boolean testFile = inventoryFile.relativePath().toString().replace('\\', '/').contains("/test/");
                files.add(new CodeFile(
                        inventoryFile.relativePath(),
                        plugin.languageId(),
                        contentHash,
                        inventoryFile.size(),
                        inventoryFile.modifiedAt(),
                        indexedAt,
                        mappedNodes.size() + 1,
                        false,
                        testFile
                ));

                if (mappedNodes.isEmpty() && extraction.errors().stream().anyMatch(error -> "error".equals(error.severity()))) {
                    filesErrored++;
                } else if (mappedNodes.isEmpty()) {
                    filesSkipped++;
                } else {
                    filesIndexed++;
                }
            }
        }

        progressConsumer.accept(new IndexProgress(IndexPhase.RESOLVING, total, total, null));
        ReferenceResolver.ResolutionResult resolution = referenceResolver.resolve(nodes, edges, unresolvedReferences);
        List<CodeUnresolvedReference> unresolved = new ArrayList<>(resolution.unresolvedReferences());

        Duration duration = Duration.ofNanos(System.nanoTime() - startedNanos);
        IndexResult indexResult = new IndexResult(
                filesIndexed > 0 || errors.stream().noneMatch(error -> "error".equals(error.severity())),
                filesIndexed,
                filesSkipped,
                filesErrored,
                nodes.size(),
                resolution.edges().size(),
                errors,
                duration
        );

        return new CodeGraph(
                project.id(),
                files,
                nodes,
                resolution.edges(),
                unresolved,
                errors,
                indexResult,
                startedAt
        );
    }

    private java.util.Optional<LanguagePlugin> findPlugin(Path projectRoot, Path absolutePath, String content) {
        SourceFile probe = new SourceFile(projectRoot, absolutePath, content);
        return languagePlugins.stream().filter(plugin -> plugin.extractor().supports(probe)).findFirst();
    }

    private static List<String> collectExtensions(List<LanguagePlugin> plugins) {
        return plugins.stream().flatMap(plugin -> plugin.supportedExtensions().stream()).distinct().toList();
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
