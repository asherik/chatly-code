package com.chatlycode.desktop.architecture;

import com.chatlycode.architecture.domain.ArchitectureSummary;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class ArchitectureDiagramView extends BorderPane {

    private final WebView webView = new WebView();

    public ArchitectureDiagramView() {
        getStyleClass().add("architecture-web-view");
        setCenter(webView);
    }

    public void setArchitecture(ArchitectureSummary architecture) {
        WebEngine engine = webView.getEngine();
        if (architecture == null) {
            engine.loadContent(emptyHtml("Open a project to see the C4 architecture diagram."));
            return;
        }
        if (architecture.structurizrJson().isBlank()) {
            engine.loadContent(emptyHtml("C4 source was generated, but Structurizr JSON is empty. Open the C4 source tab to inspect the DSL."));
            return;
        }
        String structurizrUiBase = structurizrUiBase();
        String workspaceJson = Base64.getEncoder().encodeToString(architecture.structurizrJson().getBytes(StandardCharsets.UTF_8));
        engine.loadContent(diagramHtml(structurizrUiBase, workspaceJson), "text/html");
    }

    private String structurizrUiBase() {
        URL resource = ArchitectureDiagramView.class.getResource("/structurizr-ui/");
        if (resource == null) {
            throw new IllegalStateException("Structurizr UI resources are missing");
        }
        return resource.toExternalForm();
    }

    private String diagramHtml(String structurizrUiBase, String workspaceJsonBase64) {
        return """
                <!doctype html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <script src="%1$sjs/jquery-3.6.3.min.js"></script>
                    <script src="%1$sjs/lodash-4.17.21.js"></script>
                    <script src="%1$sjs/backbone-1.4.1.js"></script>
                    <script src="%1$sjs/joint-3.6.5.js"></script>
                    <script src="%1$sjs/structurizr.js"></script>
                    <script src="%1$sjs/structurizr-util.js"></script>
                    <script src="%1$sjs/structurizr-ui.js"></script>
                    <script src="%1$sjs/structurizr-workspace.js"></script>
                    <script src="%1$sjs/structurizr-diagram.js"></script>
                    <link href="%1$scss/joint-3.6.5.css" rel="stylesheet">
                    <link href="%1$scss/structurizr.css" rel="stylesheet">
                    <style>
                        html, body { margin: 0; width: 100%%; height: 100%%; overflow: hidden; font-family: Arial, sans-serif; }
                        #diagram { position: absolute; inset: 0 0 46px 0; background: #ffffff; }
                        #toolbar {
                            position: absolute; left: 0; right: 0; bottom: 0; height: 46px;
                            display: flex; align-items: center; gap: 8px; padding: 0 12px;
                            border-top: 1px solid #d7dce3; background: rgba(255, 255, 255, 0.96);
                        }
                        button {
                            width: 32px; height: 28px; border: 1px solid #cfd6e0; border-radius: 4px;
                            background: #ffffff; color: #263244; font-size: 16px; cursor: pointer;
                        }
                        button:hover { background: #f2f6fb; }
                        select {
                            height: 28px; max-width: 360px; border: 1px solid #cfd6e0; border-radius: 4px;
                            background: #ffffff; color: #263244; font-size: 12px;
                        }
                        #title { margin-left: 8px; color: #334155; font-size: 12px; white-space: nowrap; }
                        #error { padding: 18px; color: #9f1239; font-size: 13px; }
                    </style>
                </head>
                <body>
                    <div id="diagram"></div>
                    <div id="toolbar">
                        <button id="zoomOutButton" title="Zoom out">-</button>
                        <button id="zoomInButton" title="Zoom in">+</button>
                        <button id="fitButton" title="Fit content">Fit</button>
                        <select id="viewSelector" title="C4 view"></select>
                        <span id="title">Structurizr C4 View</span>
                    </div>
                    <script>
                        function decodeUtf8Base64(value) {
                            const binary = window.atob(value);
                            if (window.TextDecoder) {
                                const bytes = new Uint8Array(binary.length);
                                for (let i = 0; i < binary.length; i++) {
                                    bytes[i] = binary.charCodeAt(i);
                                }
                                return new TextDecoder('utf-8').decode(bytes);
                            }
                            return decodeURIComponent(escape(binary));
                        }

                        try {
                            const json = decodeUtf8Base64('%2$s');
                            structurizr.workspace = new structurizr.Workspace(JSON.parse(json));
                            const viewSelector = $('#viewSelector');
                            const views = [];
                            structurizr.workspace.getViews().forEach(function(view) {
                                if (view.type === 'SystemContext' || view.type === 'Container' || view.type === 'Component') {
                                    views.push(view);
                                }
                            });
                            views.sort(function(a, b) {
                                const order = { SystemContext: 1, Container: 2, Component: 3 };
                                return (order[a.type] || 9) - (order[b.type] || 9) || a.key.localeCompare(b.key);
                            });
                            views.forEach(function(view) {
                                const label = view.type + ': ' + (view.title || view.name || view.key);
                                viewSelector.append($('<option></option>').attr('value', view.key).text(label));
                            });

                            const diagram = new structurizr.ui.Diagram('diagram', false, function() {
                                const initial = structurizr.workspace.findViewByKey('Containers') ? 'Containers' : (views[0] ? views[0].key : undefined);
                                if (initial) {
                                    viewSelector.val(initial);
                                    diagram.changeView(initial);
                                    window.setTimeout(function() { diagram.zoomFitContent(); }, 300);
                                }
                            });
                            $('#zoomOutButton').click(diagram.zoomOut);
                            $('#zoomInButton').click(diagram.zoomIn);
                            $('#fitButton').click(diagram.zoomFitContent);
                            viewSelector.change(function() {
                                diagram.changeView($(this).val(), function() {
                                    window.setTimeout(function() { diagram.zoomFitContent(); }, 150);
                                });
                            });
                        } catch (error) {
                            document.body.innerHTML = '<div id="error">Structurizr viewer failed: ' + error + '</div>';
                        }
                    </script>
                </body>
                </html>
                """.formatted(structurizrUiBase, workspaceJsonBase64);
    }

    private String emptyHtml(String message) {
        return """
                <!doctype html>
                <html>
                <body style="margin:0;font-family:Arial,sans-serif;background:#f7f9fc;color:#5b6472;">
                    <div style="padding:18px;font-size:13px;">%s</div>
                </body>
                </html>
                """.formatted(escapeHtml(message));
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
