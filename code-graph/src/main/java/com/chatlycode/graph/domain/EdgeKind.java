package com.chatlycode.graph.domain;

public enum EdgeKind {
    CONTAINS,
    IMPORTS,
    EXPORTS,
    CALLS,
    REFERENCES,
    EXTENDS,
    IMPLEMENTS,
    TYPE_OF,
    RETURNS,
    INSTANTIATES,
    OVERRIDES,
    DECORATES,
    HANDLES_ROUTE,
    READS_FROM,
    WRITES_TO,
    PUBLISHES,
    SUBSCRIBES,
    CONFIGURED_BY,
    TESTS
}
