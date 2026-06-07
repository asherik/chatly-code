package com.chatlycode.agent.tool;

import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.project.domain.OpenedProject;

public record AgentToolContext(
        String conversationId,
        String runId,
        OpenedProject project,
        CodeGraph graph
) {
}
