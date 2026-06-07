package com.chatlycode.graph.domain;

import java.nio.file.Path;

public record CodeFile(Path relativePath, String languageId, String contentHash) {
}
