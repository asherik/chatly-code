package com.chatlycode.appserver.facade;

import com.chatlycode.architecture.domain.ArchitectureSummary;
import com.chatlycode.problem.domain.DetectedProblem;
import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.task.domain.EngineeringTask;

import java.util.List;

public record DashboardSnapshot(
        OpenedProject project,
        ArchitectureSummary architecture,
        List<DetectedProblem> problems,
        List<EngineeringTask> tasks
) {

    public DashboardSnapshot {
        problems = List.copyOf(problems == null ? List.of() : problems);
        tasks = List.copyOf(tasks == null ? List.of() : tasks);
    }
}
