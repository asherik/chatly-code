package com.chatlycode.agent.event;

import com.chatlycode.agent.domain.AgentEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AgentEventStore {

    private final CopyOnWriteArrayList<AgentEvent> events = new CopyOnWriteArrayList<>();

    public void append(AgentEvent event) {
        events.add(event);
    }

    public List<AgentEvent> byRun(String runId) {
        return events.stream().filter(event -> event.runId().equals(runId)).toList();
    }

    public List<AgentEvent> all() {
        return List.copyOf(events);
    }
}
