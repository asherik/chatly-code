package com.chatlycode.architecture.application;

import com.chatlycode.graph.domain.CodeEdge;
import com.chatlycode.graph.domain.CodeFile;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.domain.EdgeKind;
import com.chatlycode.graph.domain.EdgeProvenance;
import com.chatlycode.graph.domain.IndexResult;
import com.chatlycode.graph.domain.NodeKind;
import com.chatlycode.project.domain.ProjectId;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArchitectureAnalyzerTest {

    private static final Instant NOW = Instant.parse("2026-06-10T00:00:00Z");

    @Test
    void generatesStructurizrDslAndRenderableArchitectureModel() {
        CodeNode desktop = node("desktop", "DesktopController", "desktop/src/main/java/DesktopController.java");
        CodeNode storage = node("storage", "StorageService", "core/src/storage/StorageService.java");
        CodeGraph graph = new CodeGraph(
                new ProjectId("demo-project"),
                List.of(file(desktop.filePath()), file(storage.filePath())),
                List.of(desktop, storage),
                List.of(new CodeEdge(desktop.id(), storage.id(), EdgeKind.IMPORTS, EdgeProvenance.LANGUAGE_RESOLVER, 1.0, 7, "")),
                List.of(),
                List.of(),
                new IndexResult(true, 2, 0, 0, 2, 1, List.of(), Duration.ofMillis(12)),
                NOW
        );

        var summary = new ArchitectureAnalyzer().analyze(graph);

        assertEquals(2, summary.containers().size());
        assertEquals(1, summary.relationships().size());
        assertFalse(summary.structurizrDsl().isBlank());
        assertTrue(summary.structurizrDsl().contains("workspace \"demo-project\""));
        assertTrue(summary.structurizrDsl().contains("shape RoundedBox"));
        assertTrue(summary.structurizrDsl().contains("shape Person"));
        assertTrue(summary.structurizrDsl().contains("shape Cylinder"));
        assertTrue(summary.structurizrDsl().contains("scope softwareSystem"));
        assertTrue(summary.structurizrDsl().contains("desktop -> core \"\""));
        assertTrue(summary.structurizrDsl().contains("component desktop \"Components_desktop\""));
        assertTrue(summary.structurizrDsl().contains("\"structurizr.title\" \"false\""));
        assertTrue(summary.structurizrDsl().contains("\"structurizr.metadata\" \"false\""));
        assertTrue(summary.structurizrDsl().contains("description false"));
        assertFalse(summary.structurizrDsl().contains("src_main"));
        assertFalse(summary.structurizrDsl().contains("!identifiers hierarchical"));
        assertFalse(summary.structurizrJson().isBlank());
        assertTrue(summary.structurizrJson().contains("\"key\" : \"Containers\""));
    }

    @Test
    void doesNotGenerateStructurizrWorkspaceForEmptyGraph() {
        CodeGraph graph = new CodeGraph(
                new ProjectId("empty-project"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new IndexResult(true, 0, 0, 0, 0, 0, List.of(), Duration.ofMillis(1)),
                NOW
        );

        var summary = new ArchitectureAnalyzer().analyze(graph);

        assertTrue(summary.containers().isEmpty());
        assertTrue(summary.structurizrDsl().isBlank());
        assertTrue(summary.structurizrJson().isBlank());
    }

    private static CodeNode node(String id, String name, String path) {
        return new CodeNode(
                id,
                NodeKind.CLASS,
                name,
                "com.example." + name,
                Path.of(path),
                "java",
                1,
                10,
                "",
                List.of(),
                NOW
        );
    }

    private static CodeFile file(Path path) {
        return new CodeFile(path, "java", "hash-" + path.getFileName(), 100, NOW, NOW, 1, false, false);
    }
}
