package com.chatlycode.graph.domain;

public enum EdgeProvenance {
    TREE_SITTER,
    LANGUAGE_RESOLVER,
    FRAMEWORK_RESOLVER,
    BUILD_MANIFEST,
    HEURISTIC,
    USER_CONFIRMED
}
