package com.chatlycode.task.application;

import com.chatlycode.problem.domain.DetectedProblem;
import com.chatlycode.problem.domain.ProblemSeverity;
import com.chatlycode.problem.domain.ProblemType;
import com.chatlycode.project.domain.BuildProfile;
import com.chatlycode.project.domain.DetectedStack;
import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.project.domain.ProjectId;
import com.chatlycode.shared.time.ClockProvider;
import com.chatlycode.task.domain.TaskRisk;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TaskPlannerTest {

    @Test
    void createsTaskFromLayerViolation() {
        DetectedProblem problem = new DetectedProblem(
                "problem-1",
                ProblemType.LAYER_VIOLATION,
                ProblemSeverity.ERROR,
                0.9,
                "Controller uses repository directly",
                "Move repository access to service",
                List.of("imports OrderRepository"),
                Path.of("src/OrderController.java"),
                12
        );
        TaskPlanner planner = new TaskPlanner(new ClockProvider(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)));

        var task = planner.fromProblem(problem);

        assertEquals(TaskRisk.HIGH, task.risk());
        assertFalse(task.suggestedPlan().isEmpty());
        assertEquals("problem-1", task.problemSourceId());
    }

    @Test
    void enrichesTaskWithProjectCommands() {
        OpenedProject project = new OpenedProject(
                new ProjectId("p1"),
                Path.of("C:/workspace/demo").toAbsolutePath().normalize(),
                "demo",
                Set.of(DetectedStack.GRADLE),
                new BuildProfile(List.of("gradle", "build"), List.of("gradle", "test")),
                Instant.EPOCH
        );
        TaskPlanner planner = new TaskPlanner(new ClockProvider(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)));
        var task = planner.fromProblem(new DetectedProblem(
                "problem-1",
                ProblemType.GENERIC_NAME,
                ProblemSeverity.INFO,
                0.5,
                "Utility class",
                "Rename",
                List.of("Utils"),
                Path.of("src/Util.java"),
                1
        ));

        var enriched = planner.enrichWithProjectCommands(task, project);

        assertEquals(List.of("gradle", "test"), enriched.testCommand());
        assertEquals(List.of("gradle", "build"), enriched.buildCommand());
    }
}
