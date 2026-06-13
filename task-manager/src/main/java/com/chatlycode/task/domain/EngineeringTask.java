package com.chatlycode.task.domain;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public record EngineeringTask(
        String id,
        String title,
        String goal,
        String problemSourceId,
        List<Path> affectedFiles,
        TaskRisk risk,
        List<String> suggestedPlan,
        String definitionOfDone,
        List<String> testCommand,
        List<String> buildCommand,
        TaskStatus status,
        List<String> linkedProblemIds,
        Instant createdAt
) {

    public EngineeringTask {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Task id must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title must not be blank");
        }
        goal = goal == null ? "" : goal;
        problemSourceId = problemSourceId == null ? "" : problemSourceId;
        affectedFiles = List.copyOf(affectedFiles == null ? List.of() : affectedFiles);
        risk = risk == null ? TaskRisk.MEDIUM : risk;
        suggestedPlan = List.copyOf(suggestedPlan == null ? List.of() : suggestedPlan);
        definitionOfDone = definitionOfDone == null ? "" : definitionOfDone;
        testCommand = List.copyOf(testCommand == null ? List.of() : testCommand);
        buildCommand = List.copyOf(buildCommand == null ? List.of() : buildCommand);
        status = status == null ? TaskStatus.DRAFT : status;
        linkedProblemIds = List.copyOf(linkedProblemIds == null ? List.of() : linkedProblemIds);
        if (createdAt == null) {
            throw new IllegalArgumentException("Created timestamp is required");
        }
    }
}
