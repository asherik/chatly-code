package com.chatlycode.git.cli;

import com.chatlycode.git.application.GitService;
import com.chatlycode.git.domain.GitStatus;
import com.chatlycode.runtime.application.RuntimeService;
import com.chatlycode.runtime.domain.CommandRequest;
import com.chatlycode.workspace.domain.WorkspaceRoot;

import java.time.Duration;
import java.util.List;

public final class CliGitService implements GitService {

    private final RuntimeService runtimeService;

    public CliGitService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public GitStatus status(WorkspaceRoot root) {
        String branch = run(root, List.of("git", "branch", "--show-current")).trim();
        String status = run(root, List.of("git", "status", "--porcelain=v1"));
        return new GitStatus(branch.isBlank() ? "detached" : branch, status);
    }

    @Override
    public String diff(WorkspaceRoot root) {
        return run(root, List.of("git", "diff", "--"));
    }

    @Override
    public String checkpointRef(WorkspaceRoot root) {
        return run(root, List.of("git", "rev-parse", "HEAD")).trim();
    }

    @Override
    public void rollback(WorkspaceRoot root, String checkpointRef) {
        if (checkpointRef == null || checkpointRef.isBlank()) {
            throw new IllegalArgumentException("Checkpoint ref must not be blank");
        }
        run(root, List.of("git", "reset", "--hard", checkpointRef));
        run(root, List.of("git", "clean", "-fd"));
    }

    private String run(WorkspaceRoot root, List<String> command) {
        var result = runtimeService.run(new CommandRequest(root, command, Duration.ofMinutes(2)));
        if (result.exitCode() != 0) {
            throw new IllegalStateException("Git command failed: " + result.stderr());
        }
        return result.stdout();
    }
}
