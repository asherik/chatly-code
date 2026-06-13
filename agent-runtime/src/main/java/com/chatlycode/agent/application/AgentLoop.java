package com.chatlycode.agent.application;

import com.chatlycode.agent.domain.AgentAction;
import com.chatlycode.agent.domain.AgentEvent;
import com.chatlycode.agent.domain.AgentEventType;
import com.chatlycode.agent.domain.AgentObservation;
import com.chatlycode.agent.domain.AgentRun;
import com.chatlycode.agent.domain.AgentRunStatus;
import com.chatlycode.agent.domain.ApprovalState;
import com.chatlycode.agent.domain.ToolResultStatus;
import com.chatlycode.agent.event.AgentEventStore;
import com.chatlycode.agent.tool.AgentToolContext;
import com.chatlycode.agent.tool.AgentToolRegistry;
import com.chatlycode.conversation.application.ConversationService;
import com.chatlycode.conversation.domain.MessageAuthor;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.shared.id.Ids;
import com.chatlycode.shared.time.ClockProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AgentLoop {

    private static final int MAX_FAILURES = 3;
    private static final int MAX_STEPS_PER_INVOCATION = 12;

    private final AgentToolRegistry toolRegistry;
    private final AgentEventStore eventStore;
    private final ConversationService conversationService;
    private final ClockProvider clock;

    public AgentLoop(
            AgentToolRegistry toolRegistry,
            AgentEventStore eventStore,
            ConversationService conversationService,
            ClockProvider clock
    ) {
        this.toolRegistry = toolRegistry;
        this.eventStore = eventStore;
        this.conversationService = conversationService;
        this.clock = clock;
    }

    public AgentRun executeApprovedSteps(
            AgentRun run,
            String conversationId,
            OpenedProject project,
            CodeGraph graph
    ) {
        AgentRun current = run;
        int executed = 0;
        while (executed < MAX_STEPS_PER_INVOCATION && current.currentStepIndex() < current.actions().size()) {
            AgentAction action = current.actions().get(current.currentStepIndex());
            if (action.approvalState() == ApprovalState.PENDING) {
                current = copy(current, current.status(), current.currentStepIndex(), current.failureCount());
                break;
            }
            current = executeAction(current, conversationId, project, graph, action);
            executed++;
            if (current.status() == AgentRunStatus.FAILED || current.status() == AgentRunStatus.AWAITING_REVIEW) {
                break;
            }
        }
        if (current.currentStepIndex() >= current.actions().size() && current.status() != AgentRunStatus.FAILED) {
            current = finishRun(current, conversationId, AgentRunStatus.AWAITING_REVIEW, "Agent run completed, review diff and accept or rollback");
        }
        return current;
    }

    public AgentRun executeAction(
            AgentRun run,
            String conversationId,
            OpenedProject project,
            CodeGraph graph,
            AgentAction action
    ) {
        Instant now = clock.now();
        recordEvent(run, conversationId, AgentEventType.ACTION_STARTED, MessageAuthor.TOOL, "Action: " + action.summary());
        conversationService.append(conversationId, MessageAuthor.TOOL, "Action: " + action.summary());

        AgentToolContext context = new AgentToolContext(conversationId, run.id(), project, graph);
        var toolResult = toolRegistry.require(action.type()).execute(context, action.arguments());

        AgentObservation observation = new AgentObservation(
                Ids.newId("obs"),
                action.id(),
                toolResult.status(),
                toolResult.summary(),
                toolResult.detail(),
                toolResult.exitCode(),
                now
        );
        List<AgentObservation> observations = new ArrayList<>(run.observations());
        observations.add(observation);

        List<AgentAction> actions = updateActionResult(run.actions(), action.id(), toolResult.status(), now);
        int nextIndex = run.currentStepIndex() + 1;
        int failureCount = run.failureCount();
        String failureSignature = action.type() + ":" + toolResult.summary();

        if (toolResult.status() == ToolResultStatus.FAILED) {
            failureCount += isRepeatedFailure(run, failureSignature) ? 1 : 1;
            recordEvent(run, conversationId, AgentEventType.OBSERVATION, MessageAuthor.TOOL, "Observation: " + toolResult.summary());
            conversationService.append(conversationId, MessageAuthor.TOOL, "Observation: " + toolResult.summary());
            if (failureCount >= MAX_FAILURES) {
                return finishRun(
                        copy(run, AgentRunStatus.FAILED, actions, observations, nextIndex, failureCount),
                        conversationId,
                        AgentRunStatus.FAILED,
                        "Stopped after repeated failures"
                );
            }
            return copy(run, AgentRunStatus.EXECUTING, actions, observations, nextIndex, failureCount);
        }

        recordEvent(run, conversationId, AgentEventType.OBSERVATION, MessageAuthor.TOOL, "Observation: " + toolResult.summary());
        conversationService.append(conversationId, MessageAuthor.TOOL, "Observation: " + toolResult.summary());
        recordEvent(run, conversationId, AgentEventType.ACTION_FINISHED, MessageAuthor.TOOL, "Finished: " + action.summary());
        return copy(run, AgentRunStatus.EXECUTING, actions, observations, nextIndex, 0);
    }

    public AgentRun approvePendingCommandActions(AgentRun run) {
        List<AgentAction> actions = run.actions().stream()
                .map(action -> action.approvalState() == ApprovalState.PENDING
                        ? new AgentAction(action.id(), action.type(), action.summary(), action.arguments(),
                        ApprovalState.APPROVED, action.resultStatus(), action.startedAt(), action.finishedAt())
                        : action)
                .toList();
        return copy(run, AgentRunStatus.APPROVED, actions, run.observations(), run.currentStepIndex(), run.failureCount());
    }

    private boolean isRepeatedFailure(AgentRun run, String signature) {
        long recentFailures = run.observations().stream()
                .filter(observation -> observation.status() == ToolResultStatus.FAILED)
                .filter(observation -> observation.summary().equals(signature) || signature.contains(observation.summary()))
                .count();
        return recentFailures >= 2;
    }

    private List<AgentAction> updateActionResult(
            List<AgentAction> actions,
            String actionId,
            ToolResultStatus status,
            Instant finishedAt
    ) {
        return actions.stream()
                .map(action -> action.id().equals(actionId)
                        ? new AgentAction(action.id(), action.type(), action.summary(), action.arguments(),
                        action.approvalState(), status, clock.now(), finishedAt)
                        : action)
                .toList();
    }

    private AgentRun finishRun(AgentRun run, String conversationId, AgentRunStatus status, String message) {
        recordEvent(run, conversationId, status == AgentRunStatus.FAILED ? AgentEventType.RUN_FAILED : AgentEventType.RUN_COMPLETED,
                MessageAuthor.SYSTEM, message);
        conversationService.append(conversationId, MessageAuthor.SYSTEM, message);
        return copy(run, status, run.actions(), run.observations(), run.currentStepIndex(), run.failureCount());
    }

    private void recordEvent(AgentRun run, String conversationId, AgentEventType type, MessageAuthor actor, String message) {
        eventStore.append(new AgentEvent(
                Ids.newId("event"),
                run.id(),
                conversationId,
                type,
                actor,
                message,
                clock.now()
        ));
    }

    private AgentRun copy(
            AgentRun run,
            AgentRunStatus status,
            List<AgentAction> actions,
            List<AgentObservation> observations,
            int currentStepIndex,
            int failureCount
    ) {
        List<AgentEvent> events = eventStore.byRun(run.id());
        return new AgentRun(
                run.id(),
                run.taskId(),
                run.directTask(),
                run.plan(),
                status,
                run.runtimeMode(),
                run.checkpointRef(),
                actions,
                observations,
                events,
                currentStepIndex,
                failureCount,
                run.startedAt()
        );
    }

    private AgentRun copy(AgentRun run, AgentRunStatus status, int currentStepIndex, int failureCount) {
        return copy(run, status, run.actions(), run.observations(), currentStepIndex, failureCount);
    }
}
