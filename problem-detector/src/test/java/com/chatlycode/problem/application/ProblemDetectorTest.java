package com.chatlycode.problem.application;

import com.chatlycode.graph.domain.CodeEdge;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.domain.EdgeKind;
import com.chatlycode.graph.domain.EdgeProvenance;
import com.chatlycode.graph.domain.IndexResult;
import com.chatlycode.graph.domain.NodeKind;
import com.chatlycode.problem.domain.ProblemType;
import com.chatlycode.project.domain.ProjectId;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemDetectorTest {

    @Test
    void detectsControllerRepositoryLayerViolation() {
        Path controllerPath = Path.of("src/main/java/com/example/OrderController.java");
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        CodeNode controller = new CodeNode(
                "controller-1",
                NodeKind.CLASS,
                "OrderController",
                "com.example.OrderController",
                controllerPath,
                "java",
                10,
                20,
                "",
                List.of("RestController"),
                now
        );
        CodeEdge dependsOn = new CodeEdge(
                controller.id(),
                "OrderRepository",
                EdgeKind.TYPE_OF,
                EdgeProvenance.HEURISTIC,
                0.7,
                15,
                ""
        );
        CodeGraph graph = new CodeGraph(
                new ProjectId("project-test"),
                List.of(),
                List.of(controller),
                List.of(dependsOn),
                List.of(),
                List.of(),
                new IndexResult(true, 1, 0, 0, 1, 1, List.of(), Duration.ZERO),
                now
        );

        var problems = new ProblemDetector().detect(graph);

        assertTrue(problems.stream().anyMatch(problem -> problem.type() == ProblemType.LAYER_VIOLATION));
    }
}
