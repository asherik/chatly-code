package com.chatlycode.language.spi;

import java.nio.file.Path;

public record SourceFile(Path projectRoot, Path absolutePath, String content) {

    public SourceFile {
        if (projectRoot == null || !projectRoot.isAbsolute()) {
            throw new IllegalArgumentException("Project root must be absolute");
        }
        if (absolutePath == null || !absolutePath.isAbsolute()) {
            throw new IllegalArgumentException("Source file path must be absolute");
        }
        if (!absolutePath.normalize().startsWith(projectRoot.normalize())) {
            throw new IllegalArgumentException("Source file must be inside the project root");
        }
        content = content == null ? "" : content;
    }

    public Path relativePath() {
        return projectRoot.normalize().relativize(absolutePath.normalize());
    }
}
