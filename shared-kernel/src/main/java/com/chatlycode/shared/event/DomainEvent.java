package com.chatlycode.shared.event;

import java.time.Instant;

public interface DomainEvent {

    String eventId();

    Instant occurredAt();

    String eventType();
}
