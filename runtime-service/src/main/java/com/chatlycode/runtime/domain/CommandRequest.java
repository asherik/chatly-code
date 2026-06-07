package com.chatlycode.runtime.domain;

import com.chatlycode.workspace.domain.WorkspaceRoot;

import java.time.Duration;
import java.util.List;

public record CommandRequest(WorkspaceRoot workspaceRoot, List<String> command, Duration timeout) {

    public CommandRequest {
        if (workspaceRoot == null) {
            throw new IllegalArgumentException("Workspace root is required");
        }
        command = List.copyOf(command == null ? List.of() : command);
        if (command.isEmpty()) {
            throw new IllegalArgumentException("Command must not be empty");
        }
        timeout = timeout == null ? Duration.ofMinutes(5) : timeout;
    }
}
