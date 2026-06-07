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
}
