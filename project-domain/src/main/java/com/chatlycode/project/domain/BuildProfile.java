package com.chatlycode.project.domain;

import java.util.List;

public record BuildProfile(List<String> buildCommand, List<String> testCommand) {

    public BuildProfile {
        buildCommand = List.copyOf(buildCommand == null ? List.of() : buildCommand);
        testCommand = List.copyOf(testCommand == null ? List.of() : testCommand);
    }
}
