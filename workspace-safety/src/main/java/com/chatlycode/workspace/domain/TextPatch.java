package com.chatlycode.workspace.domain;

public record TextPatch(String relativePath, String expectedText, String replacementText) {

    public TextPatch {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Relative path must not be blank");
        }
        if (expectedText == null) {
            throw new IllegalArgumentException("Expected text must not be null");
        }
        if (replacementText == null) {
            throw new IllegalArgumentException("Replacement text must not be null");
        }
    }
}
