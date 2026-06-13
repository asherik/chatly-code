package com.chatlycode.agent.policy;

import com.chatlycode.project.domain.BuildProfile;
import com.chatlycode.project.domain.DetectedStack;
import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.project.domain.ProjectId;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandPolicyServiceTest {

    @Test
    void allowsProjectTestCommandAndBlocksUnknownShell() {
        OpenedProject project = new OpenedProject(
                new ProjectId("p1"),
                Path.of("C:/workspace/demo").toAbsolutePath().normalize(),
                "demo",
                Set.of(DetectedStack.GRADLE),
                new BuildProfile(List.of("gradle", "build"), List.of("gradle", "test")),
                Instant.EPOCH
        );
        CommandPolicyService policy = new CommandPolicyService();

        assertTrue(policy.isAllowed(project, List.of("gradle", "test")));
        assertFalse(policy.isAllowed(project, List.of("rm", "-rf", "/")));
    }
}
