package com.chatlycode.agent.domain;

import java.time.Instant;
import java.util.Map;

public record AgentAction(
        String id,
        AgentActionType type,
        String summary,
        Map<String, String> arguments,
        ApprovalState approvalState,
        ToolResultStatus resultStatus,
        Instant startedAt,
        Instant finishedAt
) {

    public AgentAction {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Action id must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Action type is required");
        }
        summary = summary == null ? "" : summary;
        arguments = Map.copyOf(arguments == null ? Map.of() : arguments);
        approvalState = approvalState == null ? ApprovalState.NOT_REQUIRED : approvalState;
        resultStatus = resultStatus == null ? ToolResultStatus.SKIPPED : resultStatus;
    }
}
