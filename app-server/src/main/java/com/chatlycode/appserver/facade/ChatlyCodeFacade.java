package com.chatlycode.appserver.facade;

import com.chatlycode.architecture.application.ArchitectureAnalyzer;
import com.chatlycode.graph.application.CodeGraphIndexer;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.language.java.JavaLanguagePlugin;
import com.chatlycode.problem.application.ProblemDetector;
import com.chatlycode.project.application.InMemoryProjectRepository;
import com.chatlycode.project.application.ProjectScanner;
import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.project.port.ProjectRepository;
import com.chatlycode.shared.time.ClockProvider;
import com.chatlycode.task.application.TaskPlanner;

import java.nio.file.Path;
import java.util.List;

public final class ChatlyCodeFacade {

    private final ProjectScanner projectScanner;
    private final CodeGraphIndexer codeGraphIndexer;
    private final ArchitectureAnalyzer architectureAnalyzer;
    private final ProblemDetector problemDetector;
    private final TaskPlanner taskPlanner;

    public ChatlyCodeFacade(
            ProjectScanner projectScanner,
            CodeGraphIndexer codeGraphIndexer,
            ArchitectureAnalyzer architectureAnalyzer,
            ProblemDetector problemDetector,
            TaskPlanner taskPlanner
    ) {
        this.projectScanner = projectScanner;
        this.codeGraphIndexer = codeGraphIndexer;
        this.architectureAnalyzer = architectureAnalyzer;
        this.problemDetector = problemDetector;
        this.taskPlanner = taskPlanner;
    }

    public static ChatlyCodeFacade createDefault() {
        ClockProvider clock = ClockProvider.systemUtc();
        ProjectRepository projectRepository = new InMemoryProjectRepository();
        return new ChatlyCodeFacade(
                new ProjectScanner(projectRepository, clock),
                new CodeGraphIndexer(List.of(new JavaLanguagePlugin()), clock),
                new ArchitectureAnalyzer(),
                new ProblemDetector(),
                new TaskPlanner(clock)
        );
    }

    public DashboardSnapshot openAndScan(Path projectRoot) {
        OpenedProject project = projectScanner.open(projectRoot);
        CodeGraph graph = codeGraphIndexer.index(project);
        var architecture = architectureAnalyzer.analyze(graph);
        var problems = problemDetector.detect(graph);
        var tasks = taskPlanner.fromProblems(problems);
        return new DashboardSnapshot(project, architecture, problems, tasks);
    }
}
