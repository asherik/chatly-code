package com.chatlycode.graph.mapper;

import com.chatlycode.graph.domain.CodeNode;
import com.chatlycode.graph.domain.NodeKind;
import com.chatlycode.language.spi.ExtractedNode;

import java.nio.file.Path;
import java.time.Instant;

public final class GraphNodeMapper {

    public CodeNode toCodeNode(ExtractedNode extracted, String language, Instant updatedAt) {
        NodeKind kind = NodeKind.valueOf(extracted.kind().toUpperCase());
        String id = GraphNodeIdGenerator.generate(extracted.relativePath(), kind, extracted.name(), extracted.startLine());
        return new CodeNode(
                id,
                kind,
                extracted.name(),
                extracted.qualifiedName(),
                extracted.relativePath(),
                language,
                extracted.startLine(),
                extracted.endLine(),
                extracted.signature(),
                extracted.decorators(),
                updatedAt
        );
    }

    public CodeNode fileNode(Path relativePath, String language, Instant updatedAt) {
        String fileName = relativePath.getFileName().toString();
        String id = GraphNodeIdGenerator.generate(relativePath, NodeKind.FILE, fileName, 1);
        return new CodeNode(
                id,
                NodeKind.FILE,
                fileName,
                relativePath.toString().replace('\\', '/'),
                relativePath,
                language,
                1,
                1,
                "",
                java.util.List.of(),
                updatedAt
        );
    }

    public CodeNode packageNode(Path relativePath, String packageName, String language, Instant updatedAt) {
        String id = GraphNodeIdGenerator.generate(relativePath, NodeKind.PACKAGE, packageName, 1);
        return new CodeNode(
                id,
                NodeKind.PACKAGE,
                packageName,
                packageName,
                relativePath,
                language,
                1,
                1,
                "",
                java.util.List.of(),
                updatedAt
        );
    }
}
