package com.chatlycode.project.domain;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record OpenedProject(
        ProjectId id,
        Path root,
        String displayName,
        Set<DetectedStack> stacks,
        BuildProfile buildProfile,
        Instant openedAt
) {

    public OpenedProject {
        if (id == null) {
            throw new IllegalArgumentException("Project id is required");
        }
        if (root == null || !root.isAbsolute()) {
            throw new IllegalArgumentException("Project root must be an absolute path");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name must not be blank");
        }
        stacks = Set.copyOf(stacks == null ? Set.of() : stacks);
        if (buildProfile == null) {
            buildProfile = new BuildProfile(List.of(), List.of());
        }
        if (openedAt == null) {
            throw new IllegalArgumentException("Opened timestamp is required");
        }
    }
}
