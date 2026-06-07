package com.chatlycode.project.application;

import com.chatlycode.project.domain.BuildProfile;
import com.chatlycode.project.domain.DetectedStack;
import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.project.domain.ProjectId;
import com.chatlycode.project.port.ProjectRepository;
import com.chatlycode.shared.id.Ids;
import com.chatlycode.shared.time.ClockProvider;

import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ProjectScanner {

    private static final Set<String> IGNORE_DIRS = Set.of(
            ".git", ".chatly-code", "node_modules", "target", "build", "dist", "out",
            ".next", ".nuxt", ".svelte-kit", ".gradle", ".idea", ".vscode", "coverage"
    );

    private final ProjectRepository repository;
    private final ClockProvider clock;

    public ProjectScanner(ProjectRepository repository, ClockProvider clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public OpenedProject open(Path root) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedRoot)) {
            throw new IllegalArgumentException("Project root must be an existing directory: " + normalizedRoot);
        }

        Set<DetectedStack> stacks = detectStacks(normalizedRoot);
        OpenedProject project = new OpenedProject(
                new ProjectId(Ids.newId("project")),
                normalizedRoot,
                normalizedRoot.getFileName().toString(),
                stacks,
                buildProfile(normalizedRoot, stacks),
                clock.now()
        );
        return repository.save(project);
    }

    private Set<DetectedStack> detectStacks(Path root) {
        EnumSet<DetectedStack> stacks = EnumSet.noneOf(DetectedStack.class);
        addIfExists(stacks, root, "build.gradle", DetectedStack.GRADLE);
        addIfExists(stacks, root, "build.gradle.kts", DetectedStack.GRADLE);
        addIfExists(stacks, root, "pom.xml", DetectedStack.MAVEN);
        addIfExists(stacks, root, "Cargo.toml", DetectedStack.RUST);
        addIfExists(stacks, root, "package.json", DetectedStack.TYPESCRIPT);
        if (Files.isDirectory(root.resolve("src/main/java"))) {
            stacks.add(DetectedStack.JAVA);
        }
        if (stacks.isEmpty()) {
            stacks.add(DetectedStack.UNKNOWN);
        }
        return Set.copyOf(stacks);
    }

    private void addIfExists(Set<DetectedStack> stacks, Path root, String fileName, DetectedStack stack) {
        if (Files.isRegularFile(root.resolve(fileName))) {
            stacks.add(stack);
        }
    }

    private boolean containsFileWithExtension(Path root, String extension) {
        AtomicBoolean found = new AtomicBoolean(false);
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!dir.equals(root) && IGNORE_DIRS.contains(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return found.get() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName().toString().endsWith(extension)) {
                        found.set(true);
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            return found.get();
        } catch (IOException ignored) {
            return false;
        }
    }

    private BuildProfile buildProfile(Path root, Set<DetectedStack> stacks) {
        if (stacks.contains(DetectedStack.GRADLE)) {
            String wrapper = Files.isRegularFile(root.resolve("gradlew.bat")) ? "gradlew.bat" : "gradle";
            return new BuildProfile(List.of(wrapper, "build"), List.of(wrapper, "test"));
        }
        if (stacks.contains(DetectedStack.MAVEN)) {
            return new BuildProfile(List.of("mvn", "verify"), List.of("mvn", "test"));
        }
        if (stacks.contains(DetectedStack.RUST)) {
            return new BuildProfile(List.of("cargo", "check"), List.of("cargo", "test"));
        }
        if (stacks.contains(DetectedStack.TYPESCRIPT) || stacks.contains(DetectedStack.JAVASCRIPT)) {
            String runner = Files.isRegularFile(root.resolve("package-lock.json")) ? "npm" : "npm";
            return new BuildProfile(List.of(runner, "run", "build"), List.of(runner, "test"));
        }
        return new BuildProfile(List.of(), List.of());
    }
}
