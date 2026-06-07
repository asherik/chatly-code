package com.chatlycode.graph.mapper;

import com.chatlycode.graph.domain.NodeKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphNodeIdGeneratorTest {

    @Test
    void generatesStableCodeGraphStyleId() {
        String first = GraphNodeIdGenerator.generate(Path.of("src/App.java"), NodeKind.CLASS, "com.example.App", 10);
        String second = GraphNodeIdGenerator.generate(Path.of("src/App.java"), NodeKind.CLASS, "com.example.App", 10);

        assertEquals(first, second);
        assertTrue(first.startsWith("class:"));
        assertEquals(38, first.length());
    }
}
