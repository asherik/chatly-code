package com.chatlycode.graph.domain;

import java.nio.file.Path;

public record CodeNode(String id, String kind, String name, String qualifiedName, Path relativePath, int line) {
}
