package com.chatlycode.runtime.process;

import com.chatlycode.runtime.application.RuntimeService;
import com.chatlycode.runtime.domain.CommandRequest;
import com.chatlycode.runtime.domain.CommandResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public final class ProcessRuntimeService implements RuntimeService {

    @Override
    public CommandResult run(CommandRequest request) {
        Instant startedAt = Instant.now();
        ProcessBuilder builder = new ProcessBuilder(request.command())
                .directory(request.workspaceRoot().path().toFile())
                .redirectErrorStream(false);
        try {
            Process process = builder.start();
            boolean finished = process.waitFor(request.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(-1, "", "Command timed out", Duration.between(startedAt, Instant.now()));
            }
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            return new CommandResult(process.exitValue(), stdout, stderr, Duration.between(startedAt, Instant.now()));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start command", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Command was interrupted", exception);
        }
    }
}
