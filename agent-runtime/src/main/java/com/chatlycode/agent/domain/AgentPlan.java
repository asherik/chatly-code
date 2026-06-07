package com.chatlycode.agent.domain;

import java.util.List;

public record AgentPlan(String taskId, List<String> steps, String riskNote) {

    public AgentPlan {
        steps = List.copyOf(steps == null ? List.of() : steps);
        riskNote = riskNote == null ? "" : riskNote;
    }
}
