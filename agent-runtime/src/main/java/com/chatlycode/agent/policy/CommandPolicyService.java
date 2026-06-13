package com.chatlycode.agent.policy;

import com.chatlycode.project.domain.OpenedProject;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CommandPolicyService {

    private static final Set<String> READ_ONLY_GIT_COMMANDS = Set.of("status", "diff", "branch", "rev-parse", "log");

    public boolean isAllowed(OpenedProject project, List<String> command) {
        if (command == null || command.isEmpty()) {
            return false;
        }
        String executable = command.getFirst().toLowerCase(Locale.ROOT);
        if (isProjectBuildCommand(project, command)) {
            return true;
        }
        if ("git".equals(executable)) {
            return command.size() > 1 && READ_ONLY_GIT_COMMANDS.contains(command.get(1));
        }
        return false;
    }

    private boolean isProjectBuildCommand(OpenedProject project, List<String> command) {
        return command.equals(project.buildProfile().buildCommand())
                || command.equals(project.buildProfile().testCommand());
    }
}
