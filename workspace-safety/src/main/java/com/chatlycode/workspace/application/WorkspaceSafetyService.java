package com.chatlycode.workspace.application;

import com.chatlycode.workspace.domain.TextPatch;
import com.chatlycode.workspace.domain.WorkspaceDiff;
import com.chatlycode.workspace.domain.WorkspaceRoot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
        applyPatch(root, new TextPatch(relativePath, expectedText, replacementText));
    }

    public void applyPatch(WorkspaceRoot root, TextPatch patch) {
        Path path = root.resolveInside(patch.relativePath());
        try {
            String current = Files.readString(path, StandardCharsets.UTF_8);
            if (!current.contains(patch.expectedText())) {
                throw new IllegalStateException("Expected text was not found in " + patch.relativePath());
            }
            Files.writeString(path, current.replace(patch.expectedText(), patch.replacementText()), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to update workspace file: " + patch.relativePath(), exception);
        }
    }

    public void applyPatches(WorkspaceRoot root, List<TextPatch> patches) {
        List<TextPatch> safePatches = List.copyOf(patches == null ? List.of() : patches);
        for (TextPatch patch : safePatches) {
            applyPatch(root, patch);
        }
    }

    public WorkspaceDiff previewPatch(WorkspaceRoot root, TextPatch patch) {
        String current = readText(root, patch.relativePath());
        if (!current.contains(patch.expectedText())) {
            return new WorkspaceDiff("", false);
        }
        String updated = current.replace(patch.expectedText(), patch.replacementText());
        return new WorkspaceDiff(unifiedDiff(patch.relativePath(), current, updated), true);
    }

    public List<String> listRelativeFiles(WorkspaceRoot root) throws IOException {
        List<String> files = new ArrayList<>();
        try (var stream = Files.walk(root.path())) {
            stream.filter(Files::isRegularFile)
                    .map(path -> root.path().relativize(path).toString().replace('\\', '/'))
                    .sorted()
                    .forEach(files::add);
        }
        return List.copyOf(files);
    }

    private String unifiedDiff(String relativePath, String before, String after) {
        if (before.equals(after)) {
            return "";
        }
        return "--- " + relativePath + "\n+++ " + relativePath + "\n@@\n-"
                + before.replace("\n", "\n-")
                + "\n+"
                + after.replace("\n", "\n+");
    }
}
