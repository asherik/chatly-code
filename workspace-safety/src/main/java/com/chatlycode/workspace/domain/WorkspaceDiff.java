package com.chatlycode.workspace.domain;

public record WorkspaceDiff(String unifiedDiff, boolean hasChanges) {

    public WorkspaceDiff {
        unifiedDiff = unifiedDiff == null ? "" : unifiedDiff;
    }
}
