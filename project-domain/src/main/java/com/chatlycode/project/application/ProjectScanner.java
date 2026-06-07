package com.chatlycode.project.application;

import com.chatlycode.project.domain.BuildProfile;
import com.chatlycode.project.domain.DetectedStack;
import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.project.domain.ProjectId;
import com.chatlycode.project.port.ProjectRepository;
import com.chatlycode.shared.id.Ids;
import com.chatlycode.shared.time.ClockProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ProjectScanner {

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
        if (Files.isDirectory(root.resolve("src/main/java")) || containsFileWithExtension(root, ".java")) {
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
        try (var stream = Files.walk(root, 5)) {
            return stream.anyMatch(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(extension));
        } catch (Exception ignored) {
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
        return new BuildProfile(List.of(), List.of());
    }
}
