package com.chatlycode.graph.inventory;

import com.chatlycode.graph.domain.ExtractionError;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class FileInventoryService {

    public static final long MAX_FILE_SIZE_BYTES = 1024L * 1024L;

    private static final Set<String> DEFAULT_IGNORE_DIRS = Set.of(
            "node_modules", "bower_components", "jspm_packages", "web_modules",
            ".yarn", ".pnpm-store", ".next", ".nuxt", ".svelte-kit", ".turbo", ".vite",
            ".parcel-cache", ".angular", ".docusaurus", "storybook-static", ".vinxi", ".nitro",
            "out-tsc", ".vercel", ".netlify", ".wrangler",
            "dist", "build", "out", ".output",
            "coverage", ".nyc_output",
            "__pycache__", "__pypackages__", ".venv", "venv", ".pixi", ".pdm-build",
            ".mypy_cache", ".pytest_cache", ".ruff_cache", ".tox", ".nox", ".hypothesis",
            ".ipynb_checkpoints", ".eggs",
            "target", ".gradle", "obj", "vendor",
            ".build", "Pods", "Carthage", "DerivedData", ".swiftpm",
            ".dart_tool", ".pub-cache", ".cxx", ".externalNativeBuild", "vcpkg_installed",
            ".bloop", ".metals", "lua_modules", ".luarocks",
            "__history", "__recovery", ".cache",
            "examples", ".idea", ".vscode"
    );

    private final List<String> supportedExtensions;

    public FileInventoryService(List<String> supportedExtensions) {
        this.supportedExtensions = List.copyOf(supportedExtensions);
    }

    public List<InventoryFile> scan(Path projectRoot) {
        Path normalizedRoot = projectRoot.toAbsolutePath().normalize();
        List<InventoryFile> files = new ArrayList<>();
        try {
            Files.walkFileTree(normalizedRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(normalizedRoot)) {
                        return FileVisitResult.CONTINUE;
                    }
                    String name = dir.getFileName().toString();
                    if (name.equals(".git") || name.equals(".chatly-code")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (DEFAULT_IGNORE_DIRS.contains(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.size() > MAX_FILE_SIZE_BYTES) {
                        return FileVisitResult.CONTINUE;
                    }
                    Path relative = normalizedRoot.relativize(file);
                    if (!isSupportedSource(relative)) {
                        return FileVisitResult.CONTINUE;
                    }
                    files.add(new InventoryFile(relative, attrs.size(), attrs.lastModifiedTime().toInstant()));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan project files: " + normalizedRoot, exception);
        }
        return List.copyOf(files);
    }

    public ExtractionError sizeExceededError(Path relativePath, long size) {
        return new ExtractionError(
                "File exceeds max size (" + size + " > " + MAX_FILE_SIZE_BYTES + ")",
                relativePath,
                "warning",
                "size_exceeded"
        );
    }

    private boolean isSupportedSource(Path relativePath) {
        String fileName = relativePath.getFileName().toString();
        return supportedExtensions.stream().anyMatch(fileName::endsWith);
    }
}
