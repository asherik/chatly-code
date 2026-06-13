package com.chatlycode.graph.query;

import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.domain.IndexResult;
import com.chatlycode.graph.domain.NodeKind;
import com.chatlycode.project.domain.ProjectId;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphQueryServiceTest {

    @Test
    void answersDependencyQuestion() {
        Instant now = Instant.EPOCH;
        CodeNode service = new CodeNode(
                "service-1",
                NodeKind.CLASS,
                "OrderService",
                "com.example.OrderService",
                Path.of("src/OrderService.java"),
                "java",
                1,
                10,
                "",
                List.of("Service"),
                now
        );
        CodeGraph graph = new CodeGraph(
                new ProjectId("p1"),
                List.of(),
                List.of(service),
                List.of(),
                List.of(),
                List.of(),
                new IndexResult(true, 1, 0, 0, 1, 0, List.of(), Duration.ZERO),
                now
        );
        GraphQueryService queryService = new GraphQueryService();

        GraphAnswer answer = queryService.answerQuestion(graph, "Which files depend on OrderService?");

        assertFalse(answer.summary().isBlank());
        assertTrue(answer.relatedFiles().contains("src/OrderService.java"));
    }

    @Test
    void ranksModulesBeforeImportsInGenericAnswer() {
        Instant now = Instant.EPOCH;
        CodeNode module = new CodeNode(
                "module-1",
                NodeKind.MODULE,
                "memory",
                "core/src/memory",
                Path.of("core/src/memory/mod.rs"),
                "rust",
                1,
                1,
                "",
                List.of(),
                now
        );
        CodeNode importNode = new CodeNode(
                "import-1",
                NodeKind.IMPORT,
                "chatly_core::memory::MemoryDoc",
                "chatly_core::memory::MemoryDoc",
                Path.of("desktop/src-tauri/src/main.rs"),
                "rust",
                3,
                3,
                "",
                List.of(),
                now
        );
        CodeGraph graph = new CodeGraph(
                new ProjectId("p1"),
                List.of(),
                List.of(importNode, module),
                List.of(),
                List.of(),
                List.of(),
                new IndexResult(true, 2, 0, 0, 2, 0, List.of(), Duration.ZERO),
                now
        );
        GraphQueryService queryService = new GraphQueryService();

        GraphAnswer answer = queryService.answerQuestion(graph, "Which modules mention memory?");

        assertFalse(answer.evidence().isEmpty());
        assertTrue(answer.evidence().getFirst().startsWith("core/src/memory"));
        assertTrue(answer.relatedFiles().contains("core/src/memory/mod.rs"));
    }
}
