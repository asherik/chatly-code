package com.chatlycode.project.application;

import com.chatlycode.project.domain.DetectedStack;
import com.chatlycode.shared.time.ClockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectScannerTest {

    @TempDir
    Path tempDir;

    @Test
    void detectsGradleJavaProject() throws Exception {
        Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { java }");
        Files.createDirectories(tempDir.resolve("src/main/java/example"));
        Files.writeString(tempDir.resolve("src/main/java/example/App.java"), "package example; public class App {}");

        ProjectScanner scanner = new ProjectScanner(
                new InMemoryProjectRepository(),
                new ClockProvider(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC))
        );

        var project = scanner.open(tempDir);

        assertTrue(project.stacks().contains(DetectedStack.GRADLE));
        assertTrue(project.stacks().contains(DetectedStack.JAVA));
        assertTrue(project.buildProfile().buildCommand().contains("build"));
    }
}
