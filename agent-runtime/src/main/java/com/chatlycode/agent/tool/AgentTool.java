package com.chatlycode.agent.tool;

import com.chatlycode.agent.domain.AgentActionType;

import java.util.Map;

public interface AgentTool {

    AgentActionType type();

    AgentToolResult execute(AgentToolContext context, Map<String, String> arguments);
}
