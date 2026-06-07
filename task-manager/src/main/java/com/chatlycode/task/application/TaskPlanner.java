package com.chatlycode.task.application;

import com.chatlycode.problem.domain.DetectedProblem;
import com.chatlycode.shared.id.Ids;
import com.chatlycode.shared.time.ClockProvider;
import com.chatlycode.task.domain.EngineeringTask;
import com.chatlycode.task.domain.TaskStatus;

import java.util.List;

public final class TaskPlanner {

    private final ClockProvider clock;

    public TaskPlanner(ClockProvider clock) {
        this.clock = clock;
    }

    public List<EngineeringTask> fromProblems(List<DetectedProblem> problems) {
        return problems.stream()
                .map(problem -> new EngineeringTask(
                        Ids.newId("task"),
                        problem.title(),
                        "Resolve the problem, keep behavior covered, and review the resulting diff.",
                        TaskStatus.READY,
                        List.of(problem.id()),
                        clock.now()
                ))
                .toList();
    }
}
