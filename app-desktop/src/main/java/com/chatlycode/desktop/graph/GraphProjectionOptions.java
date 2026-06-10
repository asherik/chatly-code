package com.chatlycode.desktop.graph;

public record GraphProjectionOptions(
        GraphMode mode,
        boolean showMethods,
        boolean showImports,
        boolean showExternal,
        String searchText,
        String focusNodeId,
        int maxNodes
) {

    public GraphProjectionOptions {
        mode = mode == null ? GraphMode.ARCHITECTURE : mode;
        searchText = searchText == null ? "" : searchText.trim();
        focusNodeId = focusNodeId == null ? "" : focusNodeId;
        maxNodes = maxNodes <= 0 ? 350 : maxNodes;
    }

    public static GraphProjectionOptions defaults() {
        return new GraphProjectionOptions(GraphMode.ARCHITECTURE, false, false, true, "", "", 350);
    }
}
