package com.chatlycode.appserver.facade;

import com.chatlycode.architecture.domain.ArchitectureSummary;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.problem.domain.DetectedProblem;
import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.task.domain.EngineeringTask;

import java.util.List;

public record ProjectSession(
        OpenedProject project,
        CodeGraph graph,
        ArchitectureSummary architecture,
        List<DetectedProblem> problems,
        List<EngineeringTask> tasks,
        String conversationId,
        String gitCheckpointRef
) {

    public ProjectSession {
        problems = List.copyOf(problems == null ? List.of() : problems);
        tasks = List.copyOf(tasks == null ? List.of() : tasks);
        conversationId = conversationId == null ? "" : conversationId;
        gitCheckpointRef = gitCheckpointRef == null ? "" : gitCheckpointRef;
    }

    public DashboardSnapshot toDashboard() {
        return new DashboardSnapshot(project, architecture, problems, tasks);
    }
}
