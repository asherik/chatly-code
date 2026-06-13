package com.chatlycode.agent.domain;

import com.chatlycode.conversation.domain.MessageAuthor;

import java.time.Instant;

public record AgentEvent(
        String id,
        String runId,
        String conversationId,
        AgentEventType type,
        MessageAuthor actor,
        String message,
        Instant timestamp
) {

    public AgentEvent {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Event id must not be blank");
        }
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("Run id must not be blank");
        }
        actor = actor == null ? MessageAuthor.SYSTEM : actor;
        message = message == null ? "" : message;
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp is required");
        }
    }
}
