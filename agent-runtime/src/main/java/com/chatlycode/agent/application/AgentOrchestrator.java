package com.chatlycode.agent.application;

import com.chatlycode.agent.domain.AgentAction;
import com.chatlycode.agent.domain.AgentEvent;
import com.chatlycode.agent.domain.AgentEventType;
import com.chatlycode.agent.domain.AgentPlan;
import com.chatlycode.agent.domain.AgentRun;
import com.chatlycode.agent.domain.AgentRunStatus;
import com.chatlycode.agent.domain.RuntimeMode;
import com.chatlycode.agent.event.AgentEventStore;
import com.chatlycode.conversation.application.ConversationService;
import com.chatlycode.conversation.domain.MessageAuthor;
import com.chatlycode.git.application.GitService;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.llm.application.LlmGateway;
import com.chatlycode.problem.domain.DetectedProblem;
import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.runtime.application.RuntimeService;
import com.chatlycode.runtime.domain.CommandRequest;
import com.chatlycode.runtime.domain.CommandResult;
import com.chatlycode.shared.id.Ids;
import com.chatlycode.shared.time.ClockProvider;
import com.chatlycode.task.domain.EngineeringTask;
import com.chatlycode.workspace.domain.WorkspaceRoot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AgentOrchestrator {

    private final AgentPlanner agentPlanner;
    private final AgentActionPlanner actionPlanner;
    private final AgentContextBuilder contextBuilder;
    private final AgentLoop agentLoop;
    private final AgentEventStore eventStore;
    private final GitService gitService;
    private final RuntimeService runtimeService;
    private final ConversationService conversationService;
    private final LlmGateway llmGateway;
    private final ClockProvider clock;
    private final Map<String, AgentRun> runs = new ConcurrentHashMap<>();

    public AgentOrchestrator(
            AgentPlanner agentPlanner,
            AgentActionPlanner actionPlanner,
            AgentContextBuilder contextBuilder,
            AgentLoop agentLoop,
            AgentEventStore eventStore,
            GitService gitService,
            RuntimeService runtimeService,
            ConversationService conversationService,
            LlmGateway llmGateway,
            ClockProvider clock
    ) {
        this.agentPlanner = agentPlanner;
        this.actionPlanner = actionPlanner;
        this.contextBuilder = contextBuilder;
        this.agentLoop = agentLoop;
        this.eventStore = eventStore;
        this.gitService = gitService;
        this.runtimeService = runtimeService;
        this.conversationService = conversationService;
        this.llmGateway = llmGateway;
        this.clock = clock;
    }

    public AgentRun startRun(
            String conversationId,
            OpenedProject project,
            CodeGraph graph,
            EngineeringTask task,
            List<DetectedProblem> problems
    ) {
        return createRun(conversationId, project, graph, task, problems, "");
    }

    public AgentRun startDirectTask(
            String conversationId,
            OpenedProject project,
            CodeGraph graph,
            String directTask
    ) {
        EngineeringTask syntheticTask = new EngineeringTask(
                Ids.newId("task"),
                directTask,
                directTask,
                "",
                List.of(),
                com.chatlycode.task.domain.TaskRisk.MEDIUM,
                List.of("Inspect project", "Apply minimal safe changes", "Run verification", "Review diff"),
                "User task completed with passing verification or explicit acceptance.",
                project.buildProfile().testCommand(),
                project.buildProfile().buildCommand(),
                com.chatlycode.task.domain.TaskStatus.READY,
                List.of(),
                clock.now()
        );
        return createRun(conversationId, project, graph, syntheticTask, List.of(), directTask);
    }

    private AgentRun createRun(
            String conversationId,
            OpenedProject project,
            CodeGraph graph,
            EngineeringTask task,
            List<DetectedProblem> problems,
            String directTask
    ) {
        List<DetectedProblem> linkedProblems = problems.stream()
                .filter(problem -> task.linkedProblemIds().contains(problem.id()))
                .toList();
        AgentPlan plan = agentPlanner.plan(task, linkedProblems);
        String context = contextBuilder.build(
                graph,
                linkedProblems.isEmpty() ? problems : linkedProblems,
                task.affectedFiles().isEmpty() ? "" : task.affectedFiles().getFirst().toString()
        );

        List<AgentAction> actions = directTask.isBlank()
                ? actionPlanner.planForTask(task, project, graph, problems)
                : actionPlanner.planForDirectTask(directTask, project, graph);

        conversationService.append(conversationId, MessageAuthor.SYSTEM, "Agent run started");
        conversationService.append(conversationId, MessageAuthor.AGENT, String.join("\n", plan.steps()));
        llmGateway.complete(
                "You are a coding agent. Use graph evidence and approved workspace tools only.",
                context + "\n\nTask:\n" + (directTask.isBlank() ? task.goal() : directTask)
        );

        WorkspaceRoot workspace = new WorkspaceRoot(project.root());
        String checkpoint = safeCheckpoint(workspace);
        String runId = Ids.newId("run");
        AgentRun run = new AgentRun(
                runId,
                task.id(),
                directTask,
                plan,
                AgentRunStatus.PLANNED,
                RuntimeMode.PROCESS,
                checkpoint,
                actions,
                List.of(),
                List.of(),
                0,
                0,
                clock.now()
        );
        recordEvent(run, conversationId, AgentEventType.RUN_STARTED, "Run started with " + actions.size() + " actions");
        recordEvent(run, conversationId, AgentEventType.PLAN_CREATED, "Plan: " + String.join("; ", plan.steps()));
        runs.put(runId, withEvents(run));
        return withEvents(run);
    }

    public AgentRun approveRun(String runId, String conversationId, OpenedProject project, CodeGraph graph) {
        AgentRun run = requireRun(runId);
        AgentRun approved = agentLoop.approvePendingCommandActions(copy(run, AgentRunStatus.APPROVED));
        approved = agentLoop.executeApprovedSteps(approved, conversationId, project, graph);
        runs.put(runId, approved);
        return approved;
    }

    public AgentRun executeNextStep(String runId, String conversationId, OpenedProject project, CodeGraph graph) {
        AgentRun run = requireRun(runId);
        if (run.currentStepIndex() >= run.actions().size()) {
            return run;
        }
        AgentAction action = run.actions().get(run.currentStepIndex());
        AgentRun updated = agentLoop.executeAction(run, conversationId, project, graph, action);
        runs.put(runId, updated);
        return updated;
    }

    public String currentDiff(OpenedProject project) {
        return gitService.diff(new WorkspaceRoot(project.root()));
    }

    public CommandResult runVerification(OpenedProject project, EngineeringTask task) {
        WorkspaceRoot workspace = new WorkspaceRoot(project.root());
        List<String> command = task.testCommand().isEmpty() ? task.buildCommand() : task.testCommand();
        if (command.isEmpty()) {
            return new CommandResult(0, "No verification command detected for this project.", "", java.time.Duration.ZERO);
        }
        return runtimeService.run(new CommandRequest(workspace, command, java.time.Duration.ofMinutes(10)));
    }

    public AgentRun acceptRun(String runId, String conversationId) {
        AgentRun run = requireRun(runId);
        conversationService.append(conversationId, MessageAuthor.SYSTEM, "Changes accepted for run " + run.id());
        AgentRun accepted = copy(run, AgentRunStatus.ACCEPTED);
        recordEvent(accepted, conversationId, AgentEventType.STATE_CHANGED, "Changes accepted");
        runs.put(runId, accepted);
        return accepted;
    }

    public AgentRun rollbackRun(String runId, String conversationId, OpenedProject project) {
        AgentRun run = requireRun(runId);
        WorkspaceRoot workspace = new WorkspaceRoot(project.root());
        if (!run.checkpointRef().isBlank()) {
            gitService.rollback(workspace, run.checkpointRef());
        }
        conversationService.append(conversationId, MessageAuthor.SYSTEM, "Workspace rolled back to checkpoint " + run.checkpointRef());
        AgentRun rolledBack = copy(run, AgentRunStatus.ROLLED_BACK);
        recordEvent(rolledBack, conversationId, AgentEventType.STATE_CHANGED, "Rollback completed");
        runs.put(runId, rolledBack);
        return rolledBack;
    }

    public AgentRun getRun(String runId) {
        AgentRun run = runs.get(runId);
        return run == null ? null : withEvents(run);
    }

    public List<AgentEvent> events(String runId) {
        return eventStore.byRun(runId);
    }

    private AgentRun requireRun(String runId) {
        AgentRun run = runs.get(runId);
        if (run == null) {
            throw new IllegalArgumentException("Unknown agent run: " + runId);
        }
        return run;
    }

    private AgentRun withEvents(AgentRun run) {
        return new AgentRun(
                run.id(),
                run.taskId(),
                run.directTask(),
                run.plan(),
                run.status(),
                run.runtimeMode(),
                run.checkpointRef(),
                run.actions(),
                run.observations(),
                eventStore.byRun(run.id()),
                run.currentStepIndex(),
                run.failureCount(),
                run.startedAt()
        );
    }

    private AgentRun copy(AgentRun run, AgentRunStatus status) {
        return new AgentRun(
                run.id(),
                run.taskId(),
                run.directTask(),
                run.plan(),
                status,
                run.runtimeMode(),
                run.checkpointRef(),
                run.actions(),
                run.observations(),
                eventStore.byRun(run.id()),
                run.currentStepIndex(),
                run.failureCount(),
                run.startedAt()
        );
    }

    private void recordEvent(AgentRun run, String conversationId, AgentEventType type, String message) {
        eventStore.append(new AgentEvent(
                Ids.newId("event"),
                run.id(),
                conversationId,
                type,
                MessageAuthor.SYSTEM,
                message,
                clock.now()
        ));
    }

    private String safeCheckpoint(WorkspaceRoot workspace) {
        try {
            return gitService.checkpointRef(workspace);
        } catch (RuntimeException exception) {
            return "";
        }
    }
}
