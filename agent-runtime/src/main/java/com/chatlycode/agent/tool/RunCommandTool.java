package com.chatlycode.agent.tool;

import com.chatlycode.agent.domain.AgentActionType;
import com.chatlycode.agent.policy.CommandPolicyService;
import com.chatlycode.runtime.application.RuntimeService;
import com.chatlycode.runtime.domain.CommandRequest;
import com.chatlycode.workspace.domain.WorkspaceRoot;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class RunCommandTool implements AgentTool {

    private final RuntimeService runtimeService;
    private final CommandPolicyService commandPolicyService;

    public RunCommandTool(RuntimeService runtimeService, CommandPolicyService commandPolicyService) {
        this.runtimeService = runtimeService;
        this.commandPolicyService = commandPolicyService;
    }

    @Override
    public AgentActionType type() {
        return AgentActionType.RUN_COMMAND;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, String> arguments) {
        String commandLine = arguments.get("command");
        if (commandLine == null || commandLine.isBlank()) {
            return AgentToolResult.failed("Missing command", "command argument is required");
        }
        List<String> command = Arrays.asList(commandLine.trim().split("\\s+"));
        if (!commandPolicyService.isAllowed(context.project(), command)) {
            return AgentToolResult.failed("Command blocked by policy", String.join(" ", command));
        }
        var result = runtimeService.run(new CommandRequest(
                new WorkspaceRoot(context.project().root()),
                command,
                Duration.ofMinutes(10)
        ));
        String output = (result.stdout() + "\n" + result.stderr()).trim();
        if (result.exitCode() == 0) {
            return new AgentToolResult(
                    com.chatlycode.agent.domain.ToolResultStatus.SUCCESS,
                    "Command succeeded with exit code 0",
                    output,
                    result.exitCode()
            );
        }
        return new AgentToolResult(
                com.chatlycode.agent.domain.ToolResultStatus.FAILED,
                "Command failed with exit code " + result.exitCode(),
                output,
                result.exitCode()
        );
    }
}
