package com.chatlycode.graph.mapper;

import com.chatlycode.graph.domain.CodeEdge;
import com.chatlycode.graph.domain.EdgeKind;
import com.chatlycode.graph.domain.EdgeProvenance;
import com.chatlycode.language.spi.ExtractedEdge;

public final class GraphEdgeMapper {

    public CodeEdge toCodeEdge(ExtractedEdge extracted) {
        return new CodeEdge(
                extracted.sourceId(),
                extracted.targetId(),
                EdgeKind.valueOf(extracted.kind().toUpperCase()),
                EdgeProvenance.valueOf(extracted.provenance().toUpperCase()),
                extracted.confidence(),
                extracted.line(),
                extracted.metadataJson()
        );
    }

    public CodeEdge contains(String parentId, String childId, EdgeProvenance provenance) {
        return new CodeEdge(parentId, childId, EdgeKind.CONTAINS, provenance, 1.0, null, "");
    }

    public CodeEdge imports(String sourceId, String targetId, int line) {
        return new CodeEdge(sourceId, targetId, EdgeKind.IMPORTS, EdgeProvenance.LANGUAGE_RESOLVER, 0.9, line, "");
    }

    public CodeEdge references(String sourceId, String targetId, int line, double confidence) {
        return new CodeEdge(sourceId, targetId, EdgeKind.REFERENCES, EdgeProvenance.HEURISTIC, confidence, line, "");
    }
}
