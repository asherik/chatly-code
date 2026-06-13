package com.chatlycode.agent.tool;

import com.chatlycode.agent.domain.ToolResultStatus;

public record AgentToolResult(
        ToolResultStatus status,
        String summary,
        String detail,
        Integer exitCode
) {

    public AgentToolResult {
        status = status == null ? ToolResultStatus.FAILED : status;
        summary = summary == null ? "" : summary;
        detail = detail == null ? "" : detail;
    }

    public static AgentToolResult success(String summary, String detail) {
        return new AgentToolResult(ToolResultStatus.SUCCESS, summary, detail, null);
    }

    public static AgentToolResult failed(String summary, String detail) {
        return new AgentToolResult(ToolResultStatus.FAILED, summary, detail, null);
    }
}
