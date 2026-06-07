package com.chatlycode.agent.tool;

import com.chatlycode.agent.domain.AgentActionType;

import java.util.EnumMap;
import java.util.Map;

public final class AgentToolRegistry {

    private final Map<AgentActionType, AgentTool> tools = new EnumMap<>(AgentActionType.class);

    public AgentToolRegistry register(AgentTool tool) {
        tools.put(tool.type(), tool);
        return this;
    }

    public AgentTool require(AgentActionType type) {
        AgentTool tool = tools.get(type);
        if (tool == null) {
            throw new IllegalStateException("No tool registered for " + type);
        }
        return tool;
    }
}
