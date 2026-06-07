package com.chatlycode.task.domain;

import java.time.Instant;
import java.util.List;

public record EngineeringTask(
        String id,
        String title,
        String definitionOfDone,
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
        definitionOfDone = definitionOfDone == null ? "" : definitionOfDone;
        status = status == null ? TaskStatus.DRAFT : status;
        linkedProblemIds = List.copyOf(linkedProblemIds == null ? List.of() : linkedProblemIds);
        if (createdAt == null) {
            throw new IllegalArgumentException("Created timestamp is required");
        }
    }
}
