package com.chatlycode.agent.application;

import com.chatlycode.agent.domain.AgentActionType;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.IndexResult;
import com.chatlycode.project.domain.BuildProfile;
import com.chatlycode.project.domain.DetectedStack;
import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.project.domain.ProjectId;
import com.chatlycode.task.domain.EngineeringTask;
import com.chatlycode.task.domain.TaskRisk;
import com.chatlycode.task.domain.TaskStatus;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentActionPlannerTest {

    @Test
    void plansOpenHandsStyleInspectAndVerifyFlow() {
        OpenedProject project = new OpenedProject(
                new ProjectId("p1"),
                Path.of("C:/workspace/demo").toAbsolutePath().normalize(),
                "demo",
                Set.of(DetectedStack.GRADLE),
                new BuildProfile(List.of("gradle", "build"), List.of("gradle", "test")),
                Instant.EPOCH
        );
        CodeGraph graph = new CodeGraph(
                new ProjectId("p1"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new IndexResult(true, 0, 0, 0, 0, 0, List.of(), Duration.ZERO),
                Instant.EPOCH
        );
        EngineeringTask task = new EngineeringTask(
                "task-1",
                "Fix controller",
                "Move repository access to service",
                "",
                List.of(Path.of("src/OrderController.java")),
                TaskRisk.HIGH,
                List.of(),
                "done",
                List.of("gradle", "test"),
                List.of("gradle", "build"),
                TaskStatus.READY,
                List.of(),
                Instant.EPOCH
        );

        var actions = new AgentActionPlanner().planForTask(task, project, graph, List.of());

        assertTrue(actions.stream().anyMatch(action -> action.type() == AgentActionType.GIT_STATUS));
        assertTrue(actions.stream().anyMatch(action -> action.type() == AgentActionType.READ_FILE));
        assertTrue(actions.stream().anyMatch(action -> action.type() == AgentActionType.RUN_COMMAND));
        assertTrue(actions.stream().anyMatch(action -> action.type() == AgentActionType.GIT_DIFF));
    }
}
