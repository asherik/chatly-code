package com.chatlycode.desktop.graph;

public record GraphProjectionOptions(
        GraphMode mode,
        boolean showMethods,
        boolean showImports,
        boolean showExternal,
        boolean projectOnly,
        GraphProblemFilter problemFilter,
        String searchText,
        String focusNodeId,
        int maxNodes
) {

    public GraphProjectionOptions {
        mode = mode == null ? GraphMode.ARCHITECTURE : mode;
        problemFilter = problemFilter == null ? GraphProblemFilter.ALL : problemFilter;
        searchText = searchText == null ? "" : searchText.trim();
        focusNodeId = focusNodeId == null ? "" : focusNodeId;
        maxNodes = maxNodes <= 0 ? 350 : maxNodes;
    }

    public static GraphProjectionOptions defaults() {
        return new GraphProjectionOptions(GraphMode.ARCHITECTURE, false, false, true, true, GraphProblemFilter.ALL, "", "", 350);
    }
}
