package com.chatlycode.task.application;

import com.chatlycode.problem.domain.DetectedProblem;
import com.chatlycode.problem.domain.ProblemSeverity;
import com.chatlycode.problem.domain.ProblemType;
import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.shared.id.Ids;
import com.chatlycode.shared.time.ClockProvider;
import com.chatlycode.task.domain.EngineeringTask;
import com.chatlycode.task.domain.TaskRisk;
import com.chatlycode.task.domain.TaskStatus;

import java.util.ArrayList;
import java.util.List;

public final class TaskPlanner {

    private final ClockProvider clock;

    public TaskPlanner(ClockProvider clock) {
        this.clock = clock;
    }

    public List<EngineeringTask> fromProblems(List<DetectedProblem> problems) {
        return problems.stream().map(this::fromProblem).toList();
    }

    public EngineeringTask fromProblem(DetectedProblem problem) {
        return new EngineeringTask(
                Ids.newId("task"),
                taskTitle(problem),
                taskGoal(problem),
                problem.id(),
                List.of(problem.primaryPath()),
                taskRisk(problem),
                suggestedPlan(problem),
                definitionOfDone(problem),
                List.of(),
                List.of(),
                TaskStatus.READY,
                List.of(problem.id()),
                clock.now()
        );
    }

    public EngineeringTask enrichWithProjectCommands(EngineeringTask task, OpenedProject project) {
        return new EngineeringTask(
                task.id(),
                task.title(),
                task.goal(),
                task.problemSourceId(),
                task.affectedFiles(),
                task.risk(),
                task.suggestedPlan(),
                task.definitionOfDone(),
                project.buildProfile().testCommand(),
                project.buildProfile().buildCommand(),
                task.status(),
                task.linkedProblemIds(),
                task.createdAt()
        );
    }

    private String taskTitle(DetectedProblem problem) {
        return switch (problem.type()) {
            case LAYER_VIOLATION -> "Move repository access out of controller";
            case ENTITY_LEAK -> "Replace entity exposure with DTO/view model";
            case HUGE_CLASS -> "Split oversized class by responsibility";
            case TOO_MANY_DEPENDENCIES -> "Reduce dependencies for " + problem.primaryPath().getFileName();
            case HIGH_BLAST_RADIUS -> "Add safety checks before changing high-impact type";
            default -> problem.title();
        };
    }

    private String taskGoal(DetectedProblem problem) {
        return switch (problem.type()) {
            case LAYER_VIOLATION -> "Introduce or extend a service and route repository access through it.";
            case ENTITY_LEAK -> "Keep persistence entities inside the persistence boundary.";
            case HUGE_CLASS -> "Extract cohesive parts into dedicated types without behavior regressions.";
            case HIGH_BLAST_RADIUS -> "Document impact radius and ensure verification commands pass.";
            default -> problem.description();
        };
    }

    private TaskRisk taskRisk(DetectedProblem problem) {
        if (problem.severity() == ProblemSeverity.ERROR || problem.type() == ProblemType.HIGH_BLAST_RADIUS) {
            return TaskRisk.HIGH;
        }
        if (problem.severity() == ProblemSeverity.WARNING) {
            return TaskRisk.MEDIUM;
        }
        return TaskRisk.LOW;
    }

    private List<String> suggestedPlan(DetectedProblem problem) {
        List<String> steps = new ArrayList<>();
        steps.add("Review graph evidence: " + String.join("; ", problem.evidence()));
        steps.add("Prepare a minimal patch in " + problem.primaryPath());
        steps.add("Run project test command");
        steps.add("Review diff and accept or rollback");
        return List.copyOf(steps);
    }

    private String definitionOfDone(DetectedProblem problem) {
        return "Problem '" + problem.title() + "' is resolved, behavior is preserved, and verification commands pass.";
    }
}
