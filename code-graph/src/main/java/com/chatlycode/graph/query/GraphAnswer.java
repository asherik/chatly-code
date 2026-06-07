package com.chatlycode.graph.query;

import java.util.List;

public record GraphAnswer(
        String question,
        String summary,
        List<String> evidence,
        List<String> relatedFiles,
        boolean partial
) {

    public GraphAnswer {
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
        relatedFiles = List.copyOf(relatedFiles == null ? List.of() : relatedFiles);
    }
}
