package com.chatlycode.project.domain;

public record ProjectId(String value) {

    public ProjectId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Project id must not be blank");
        }
    }
}
