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
        if (architecture.fileCount() == 0 || architecture.containers().isEmpty()) {
            engine.loadContent(emptyHtml("No indexed code was found for this project. Open the repository root instead of a service folder such as .idea."));
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
                        body { background: #ffffff; color: #111827; }
                        #titleBar {
                            position: absolute; left: 0; right: 0; top: 0; height: 30px;
                            display: flex; align-items: center; gap: 10px; padding: 0 10px;
                            border-bottom: 1px solid #eeeeee; box-sizing: border-box; background: #ffffff;
                            z-index: 10;
                        }
                        #title {
                            flex: 1 1 auto; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
                            color: #111827; font-size: 18px; line-height: 30px; font-weight: 400;
                        }
                        #diagram { position: absolute; inset: 30px 0 0 0; background: #ffffff; }
                        #toolbar {
                            position: absolute; right: 28px; bottom: 26px;
                            display: flex; align-items: center; gap: 8px; padding: 0;
                            background: transparent; z-index: 20;
                        }
                        button {
                            width: 54px; height: 48px; border: 1px solid #edf0f3; border-radius: 8px;
                            background: rgba(255, 255, 255, 0.82); color: #a3aab5; font-size: 20px; cursor: pointer;
                            box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
                        }
                        button:hover { background: #ffffff; color: #5b6472; border-color: #d8dee6; }
                        select {
                            flex: 0 0 auto; height: 24px; max-width: 260px; border: 1px solid #eeeeee; border-radius: 5px;
                            background: #ffffff; color: #6b7280; font-size: 12px; opacity: 0.72;
                        }
                        select:hover, select:focus { opacity: 1; border-color: #d8dee6; outline: none; }
                        .structurizrDiagramTitle,
                        .structurizrDiagramDescription,
                        .structurizrDiagramMetadata { display: none !important; }
                        .structurizrNavigation { display: none !important; }
                        .structurizrDiagramViewport { overflow: hidden !important; }
                        #error { padding: 18px; color: #9f1239; font-size: 13px; }
                    </style>
                </head>
                <body>
                    <div id="titleBar">
                        <div id="title">Structurizr C4 View</div>
                        <select id="viewSelector" title="C4 view"></select>
                    </div>
                    <div id="diagram"></div>
                    <div id="toolbar">
                        <button id="zoomOutButton" title="Zoom out">-</button>
                        <button id="zoomInButton" title="Zoom in">+</button>
                        <button id="fitButton" title="Fit content">&#9633;</button>
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
                                const label = displayNameForView(view);
                                viewSelector.append($('<option></option>').attr('value', view.key).text(label));
                            });
                            if (views.length <= 1) {
                                viewSelector.hide();
                            }

                            function updateTitle(viewKey) {
                                const view = structurizr.workspace.findViewByKey(viewKey);
                                if (!view) {
                                    $('#title').text('Structurizr C4 View');
                                    return;
                                }
                                const type = view.type === 'Container' ? 'Container View'
                                        : view.type === 'SystemContext' ? 'System Context View'
                                        : view.type === 'Component' ? 'Component View'
                                        : view.type + ' View';
                                const name = displayNameForView(view);
                                if (name.startsWith(type + ': ')) {
                                    $('#title').text(name + ' (#' + view.key + ')');
                                } else {
                                    $('#title').text(type + ': ' + name + ' (#' + view.key + ')');
                                }
                            }

                            function displayNameForView(view) {
                                let name = view.name || view.title || view.key;
                                const prefixes = ['Container View: ', 'System Context View: ', 'Component View: '];
                                prefixes.forEach(function(prefix) {
                                    if (name.startsWith(prefix)) {
                                        name = name.substring(prefix.length);
                                    }
                                });
                                return name;
                            }

                            function fitDiagram() {
                                window.setTimeout(function() { diagram.zoomFitContent(); }, 120);
                                window.setTimeout(function() {
                                    diagram.zoomFitContent();
                                    diagram.zoomOut();
                                    diagram.zoomOut();
                                }, 420);
                                window.setTimeout(function() {
                                    $('.structurizrDiagramTitle,.structurizrDiagramDescription,.structurizrDiagramMetadata').hide();
                                }, 520);
                            }

                            const diagram = new structurizr.ui.Diagram('diagram', false, function() {
                                const initial = structurizr.workspace.findViewByKey('Containers') ? 'Containers' : (views[0] ? views[0].key : undefined);
                                if (initial) {
                                    viewSelector.val(initial);
                                    updateTitle(initial);
                                    diagram.changeView(initial);
                                    fitDiagram();
                                }
                            });
                            $('#zoomOutButton').click(diagram.zoomOut);
                            $('#zoomInButton').click(diagram.zoomIn);
                            $('#fitButton').click(fitDiagram);
                            viewSelector.change(function() {
                                const viewKey = $(this).val();
                                updateTitle(viewKey);
                                diagram.changeView(viewKey, function() {
                                    fitDiagram();
                                });
                            });
                            let resizeTimer;
                            window.addEventListener('resize', function() {
                                window.clearTimeout(resizeTimer);
                                resizeTimer = window.setTimeout(fitDiagram, 180);
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
