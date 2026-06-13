package com.chatlycode.appserver.facade;

import com.chatlycode.agent.application.AgentContextBuilder;
import com.chatlycode.agent.application.AgentOrchestrator;
import com.chatlycode.agent.config.AgentRuntimeFactory;
import com.chatlycode.agent.domain.AgentEvent;
import com.chatlycode.agent.domain.AgentRun;
import com.chatlycode.architecture.application.ArchitectureAnalyzer;
import com.chatlycode.conversation.application.ConversationService;
import com.chatlycode.conversation.domain.ConversationMessage;
import com.chatlycode.conversation.domain.MessageAuthor;
import com.chatlycode.git.application.GitService;
import com.chatlycode.git.cli.CliGitService;
import com.chatlycode.git.domain.GitStatus;
import com.chatlycode.graph.application.CodeGraphIndexer;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.graph.domain.IndexProgress;
import com.chatlycode.graph.query.GraphAnswer;
import com.chatlycode.graph.query.GraphQueryService;
import com.chatlycode.language.generic.GenericLanguagePlugin;
import com.chatlycode.language.java.JavaLanguagePlugin;
import com.chatlycode.llm.application.LlmGateway;
import com.chatlycode.llm.application.LlmGatewayStatus;
import com.chatlycode.llm.provider.LlmGatewayFactory;
import com.chatlycode.problem.application.ProblemDetector;
import com.chatlycode.project.application.InMemoryProjectRepository;
import com.chatlycode.project.application.ProjectScanner;
import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.project.port.ProjectRepository;
import com.chatlycode.runtime.domain.CommandResult;
import com.chatlycode.runtime.process.ProcessRuntimeService;
import com.chatlycode.shared.id.Ids;
import com.chatlycode.shared.time.ClockProvider;
import com.chatlycode.task.application.TaskPlanner;
import com.chatlycode.task.domain.EngineeringTask;
import com.chatlycode.workspace.application.WorkspaceSafetyService;
import com.chatlycode.workspace.domain.WorkspaceRoot;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ChatlyCodeFacade {

    private final ProjectScanner projectScanner;
    private final CodeGraphIndexer codeGraphIndexer;
    private final ArchitectureAnalyzer architectureAnalyzer;
    private final ProblemDetector problemDetector;
    private final TaskPlanner taskPlanner;
    private final GraphQueryService graphQueryService;
    private final ConversationService conversationService;
    private final AgentOrchestrator agentOrchestrator;
    private final GitService gitService;
    private final WorkspaceSafetyService workspaceSafetyService;
    private final LlmGateway llmGateway;
    private final ClockProvider clock;
    private final Map<String, ProjectSession> sessions = new ConcurrentHashMap<>();

    public ChatlyCodeFacade(
            ProjectScanner projectScanner,
            CodeGraphIndexer codeGraphIndexer,
            ArchitectureAnalyzer architectureAnalyzer,
            ProblemDetector problemDetector,
            TaskPlanner taskPlanner,
            GraphQueryService graphQueryService,
            ConversationService conversationService,
            AgentOrchestrator agentOrchestrator,
            GitService gitService,
            WorkspaceSafetyService workspaceSafetyService,
            LlmGateway llmGateway,
            ClockProvider clock
    ) {
        this.projectScanner = projectScanner;
        this.codeGraphIndexer = codeGraphIndexer;
        this.architectureAnalyzer = architectureAnalyzer;
        this.problemDetector = problemDetector;
        this.taskPlanner = taskPlanner;
        this.graphQueryService = graphQueryService;
        this.conversationService = conversationService;
        this.agentOrchestrator = agentOrchestrator;
        this.gitService = gitService;
        this.workspaceSafetyService = workspaceSafetyService;
        this.llmGateway = llmGateway;
        this.clock = clock;
    }

    public static ChatlyCodeFacade createDefault() {
        ClockProvider clock = ClockProvider.systemUtc();
        ProjectRepository projectRepository = new InMemoryProjectRepository();
        var runtimeService = new ProcessRuntimeService();
        var conversationService = new ConversationService(clock);
        var workspaceSafetyService = new WorkspaceSafetyService();
        var gitService = new CliGitService(runtimeService);
        var llmGateway = LlmGatewayFactory.createFromEnvironment();
        return new ChatlyCodeFacade(
                new ProjectScanner(projectRepository, clock),
                new CodeGraphIndexer(List.of(
                        new JavaLanguagePlugin(),
                        GenericLanguagePlugin.rust(),
                        GenericLanguagePlugin.typescript(),
                        GenericLanguagePlugin.javascript()
                ), clock),
                new ArchitectureAnalyzer(),
                new ProblemDetector(),
                new TaskPlanner(clock),
                new GraphQueryService(),
                conversationService,
                AgentRuntimeFactory.create(
                        workspaceSafetyService,
                        gitService,
                        runtimeService,
                        conversationService,
                        llmGateway,
                        clock
                ),
                gitService,
                workspaceSafetyService,
                llmGateway,
                clock
        );
    }

    public ProjectSession openAndScan(Path projectRoot) {
        return openAndScan(projectRoot, ignored -> {});
    }

    public ProjectSession openAndScan(Path projectRoot, Consumer<IndexProgress> progressConsumer) {
        OpenedProject project = projectScanner.open(projectRoot);
        CodeGraph graph = codeGraphIndexer.index(project, progressConsumer == null ? ignored -> {} : progressConsumer);
        var architecture = architectureAnalyzer.analyze(graph);
        var problems = problemDetector.detect(graph, project.root());
        var tasks = taskPlanner.fromProblems(problems).stream()
                .map(task -> taskPlanner.enrichWithProjectCommands(task, project))
                .toList();
        String conversationId = Ids.newId("conversation");
        conversationService.append(conversationId, MessageAuthor.SYSTEM, "Project opened: " + project.displayName());

        WorkspaceRoot workspace = new WorkspaceRoot(project.root());
        String checkpoint = safeCheckpoint(workspace);
        ProjectSession session = new ProjectSession(
                project,
                graph,
                architecture,
                problems,
                tasks,
                conversationId,
                checkpoint
        );
        sessions.put(project.id().value(), session);
        return session;
    }

    public ProjectSession currentSession(OpenedProject project) {
        return sessions.get(project.id().value());
    }

    public DashboardSnapshot dashboard(ProjectSession session) {
        return session.toDashboard();
    }

    public GitStatus gitStatus(ProjectSession session) {
        return gitService.status(new WorkspaceRoot(session.project().root()));
    }

    public String workspaceDiff(ProjectSession session) {
        return gitService.diff(new WorkspaceRoot(session.project().root()));
    }

    public GraphAnswer askGraph(ProjectSession session, String question) {
        GraphAnswer answer = graphQueryService.answerQuestion(session.graph(), question);
        conversationService.append(session.conversationId(), MessageAuthor.USER, question);
        conversationService.append(session.conversationId(), MessageAuthor.AGENT, answer.summary());
        return answer;
    }

    public List<ConversationMessage> conversation(ProjectSession session) {
        return conversationService.history(session.conversationId());
    }

    public LlmGatewayStatus llmStatus() {
        return llmGateway.status();
    }

    public AgentRun startAgentRun(ProjectSession session, EngineeringTask task) {
        return agentOrchestrator.startRun(
                session.conversationId(),
                session.project(),
                session.graph(),
                task,
                session.problems()
        );
    }

    public AgentRun approveAgentRun(ProjectSession session, String runId) {
        return agentOrchestrator.approveRun(runId, session.conversationId(), session.project(), session.graph());
    }

    public AgentRun executeAgentStep(ProjectSession session, String runId) {
        return agentOrchestrator.executeNextStep(runId, session.conversationId(), session.project(), session.graph());
    }

    public AgentRun submitDirectTask(ProjectSession session, String taskText) {
        return agentOrchestrator.startDirectTask(
                session.conversationId(),
                session.project(),
                session.graph(),
                taskText
        );
    }

    public List<AgentEvent> agentEvents(String runId) {
        return agentOrchestrator.events(runId);
    }

    public CommandResult verifyTask(ProjectSession session, EngineeringTask task) {
        CommandResult result = agentOrchestrator.runVerification(session.project(), task);
        conversationService.append(
                session.conversationId(),
                MessageAuthor.SYSTEM,
                "Verification exit code: " + result.exitCode()
        );
        return result;
    }

    public AgentRun acceptAgentRun(ProjectSession session, String runId) {
        return agentOrchestrator.acceptRun(runId, session.conversationId());
    }

    public AgentRun rollbackAgentRun(ProjectSession session, String runId) {
        return agentOrchestrator.rollbackRun(runId, session.conversationId(), session.project());
    }

    public String explainWithLlm(ProjectSession session, String userPrompt) {
        String context = new AgentContextBuilder().build(session.graph(), session.problems(), "");
        String response = safeLlmComplete(
                "Answer using graph evidence only. Mark uncertainty when evidence is incomplete.",
                context + "\n\nQuestion:\n" + userPrompt
        );
        conversationService.append(session.conversationId(), MessageAuthor.USER, userPrompt);
        conversationService.append(session.conversationId(), MessageAuthor.AGENT, response);
        return response;
    }

    public String readWorkspaceFile(ProjectSession session, String relativePath) {
        return workspaceSafetyService.readText(new WorkspaceRoot(session.project().root()), relativePath);
    }

    private String safeCheckpoint(WorkspaceRoot workspace) {
        try {
            return gitService.checkpointRef(workspace);
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private String safeLlmComplete(String systemPrompt, String userPrompt) {
        try {
            return llmGateway.complete(systemPrompt, userPrompt);
        } catch (RuntimeException exception) {
            return "LLM request failed: " + exception.getMessage();
        }
    }
}
