package com.chatlycode.agent.domain;

import java.time.Instant;
import java.util.List;

public record AgentRun(
        String id,
        String taskId,
        String directTask,
        AgentPlan plan,
        AgentRunStatus status,
        RuntimeMode runtimeMode,
        String checkpointRef,
        List<AgentAction> actions,
        List<AgentObservation> observations,
        List<AgentEvent> events,
        int currentStepIndex,
        int failureCount,
        Instant startedAt
) {

    public AgentRun {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Agent run id must not be blank");
        }
        directTask = directTask == null ? "" : directTask;
        status = status == null ? AgentRunStatus.PLANNED : status;
        runtimeMode = runtimeMode == null ? RuntimeMode.PROCESS : runtimeMode;
        checkpointRef = checkpointRef == null ? "" : checkpointRef;
        actions = List.copyOf(actions == null ? List.of() : actions);
        observations = List.copyOf(observations == null ? List.of() : observations);
        events = List.copyOf(events == null ? List.of() : events);
        if (startedAt == null) {
            throw new IllegalArgumentException("Started timestamp is required");
        }
    }

    public List<String> observationSummaries() {
        return observations.stream().map(AgentObservation::summary).toList();
    }
}
