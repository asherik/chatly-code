package com.chatlycode.workspace.application;

import com.chatlycode.workspace.domain.WorkspaceRoot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorkspaceSafetyService {

    public String readText(WorkspaceRoot root, String relativePath) {
        Path path = root.resolveInside(relativePath);
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read workspace file: " + relativePath, exception);
        }
    }

    public void replaceText(WorkspaceRoot root, String relativePath, String expectedText, String replacementText) {
        Path path = root.resolveInside(relativePath);
        try {
            String current = Files.readString(path, StandardCharsets.UTF_8);
            if (!current.contains(expectedText)) {
                throw new IllegalStateException("Expected text was not found in " + relativePath);
            }
            Files.writeString(path, current.replace(expectedText, replacementText), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to update workspace file: " + relativePath, exception);
        }
    }
}
