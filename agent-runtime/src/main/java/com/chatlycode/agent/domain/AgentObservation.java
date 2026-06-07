package com.chatlycode.agent.domain;

import java.time.Instant;

public record AgentObservation(
        String id,
        String actionId,
        ToolResultStatus status,
        String summary,
        String detail,
        Integer exitCode,
        Instant observedAt
) {

    public AgentObservation {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Observation id must not be blank");
        }
        if (actionId == null || actionId.isBlank()) {
            throw new IllegalArgumentException("Action id must not be blank");
        }
        status = status == null ? ToolResultStatus.FAILED : status;
        summary = summary == null ? "" : summary;
        detail = detail == null ? "" : detail;
        if (observedAt == null) {
            throw new IllegalArgumentException("Observed timestamp is required");
        }
    }
}
