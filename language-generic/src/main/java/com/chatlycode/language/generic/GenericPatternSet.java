package com.chatlycode.language.generic;

import java.util.List;
import java.util.regex.Pattern;

record GenericPatternSet(
        List<NodePattern> nodePatterns,
        Pattern importPattern,
        Pattern modulePattern
) {

    static GenericPatternSet rust() {
        return new GenericPatternSet(
                List.of(
                        new NodePattern("function", Pattern.compile("^\\s*(?:pub\\s+)?(?:async\\s+)?fn\\s+([A-Za-z_][\\w]*)\\s*\\(")),
                        new NodePattern("class", Pattern.compile("^\\s*(?:pub\\s+)?struct\\s+([A-Za-z_][\\w]*)\\b")),
                        new NodePattern("enum", Pattern.compile("^\\s*(?:pub\\s+)?enum\\s+([A-Za-z_][\\w]*)\\b")),
                        new NodePattern("interface", Pattern.compile("^\\s*(?:pub\\s+)?trait\\s+([A-Za-z_][\\w]*)\\b"))
                ),
                Pattern.compile("^\\s*use\\s+([^;]+);"),
                Pattern.compile("^\\s*(?:pub\\s+)?mod\\s+([A-Za-z_][\\w]*)\\s*;")
        );
    }

    static GenericPatternSet typescript() {
        return new GenericPatternSet(
                List.of(
                        new NodePattern("function", Pattern.compile("^\\s*(?:export\\s+)?(?:async\\s+)?function\\s+([A-Za-z_$][\\w$]*)\\s*\\(")),
                        new NodePattern("function", Pattern.compile("^\\s*(?:export\\s+)?(?:const|let)\\s+([A-Za-z_$][\\w$]*)\\s*=\\s*(?:async\\s*)?\\([^)]*\\)\\s*=>")),
                        new NodePattern("class", Pattern.compile("^\\s*(?:export\\s+)?class\\s+([A-Za-z_$][\\w$]*)\\b")),
                        new NodePattern("interface", Pattern.compile("^\\s*(?:export\\s+)?interface\\s+([A-Za-z_$][\\w$]*)\\b")),
                        new NodePattern("enum", Pattern.compile("^\\s*(?:export\\s+)?enum\\s+([A-Za-z_$][\\w$]*)\\b"))
                ),
                Pattern.compile("^\\s*import\\s+.*?from\\s+['\"]([^'\"]+)['\"]|^\\s*import\\s+['\"]([^'\"]+)['\"]"),
                Pattern.compile("^\\s*(?:export\\s+)?namespace\\s+([A-Za-z_$][\\w$]*)\\b")
        );
    }

    record NodePattern(String kind, Pattern pattern) {
    }
}
