package com.chatlycode.desktop.controller;

import com.chatlycode.appserver.facade.ChatlyCodeFacade;
import com.chatlycode.appserver.facade.ProjectSession;
import com.chatlycode.desktop.architecture.ArchitectureDiagramView;
import com.chatlycode.desktop.graph.CodeGraphProjectionService;
import com.chatlycode.desktop.graph.GraphCanvas;
import com.chatlycode.desktop.graph.GraphMode;
import com.chatlycode.desktop.graph.GraphProblemFilter;
import com.chatlycode.desktop.graph.GraphProjection;
import com.chatlycode.desktop.graph.GraphProjectionOptions;
import com.chatlycode.desktop.graph.GraphVertex;
import com.chatlycode.desktop.viewmodel.MainViewModel;
import com.chatlycode.i18n.LocalizationService;
import com.chatlycode.problem.domain.DetectedProblem;
import com.chatlycode.task.domain.EngineeringTask;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

public final class MainController {

    private final ChatlyCodeFacade facade;
    private final LocalizationService localization;
    private final Locale locale;
    private final MainViewModel viewModel = new MainViewModel();
    private final CodeGraphProjectionService graphProjectionService = new CodeGraphProjectionService();
    private final BorderPane root = new BorderPane();
    private final Label status = new Label();

    public MainController(ChatlyCodeFacade facade, LocalizationService localization, Locale locale) {
        this.facade = facade;
        this.localization = localization;
        this.locale = locale == null ? Locale.ENGLISH : locale;
        applyLlmStatus();
        buildView();
    }

    public Parent view() {
        return root;
    }

    private void buildView() {
        Button openProject = new Button(text("action.openProject"));
        openProject.setOnAction(event -> chooseAndScanProject());

        Label title = new Label(text("app.title"));
        title.getStyleClass().add("app-title");

        Label branch = new Label();
        branch.textProperty().bind(viewModel.gitBranchProperty());
        branch.getStyleClass().add("git-branch");

        Label llm = new Label();
        llm.textProperty().bind(viewModel.llmStatusProperty());
        llm.getStyleClass().add("llm-status");

        HBox toolbar = new HBox(12, title, openProject, branch, llm);
        toolbar.getStyleClass().add("toolbar");
        toolbar.setPadding(new Insets(12));

        TabPane tabs = new TabPane(
                overviewTab(),
                graphTab(),
                problemsTab(),
                tasksTab(),
                agentRunTab(),
                chatTab(),
                diffTab(),
                terminalTab()
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        status.setText(text("status.ready"));
        status.getStyleClass().add("status");

        root.setTop(toolbar);
        root.setCenter(tabs);
        root.setBottom(status);
    }

    private Tab overviewTab() {
        GridPane metrics = new GridPane();
        metrics.getStyleClass().add("metrics");
        metrics.setHgap(12);
        metrics.setVgap(12);
        var dashboard = viewModel.dashboard();
        addMetric(metrics, 0, text("dashboard.files"), dashboard.filesProperty().asString());
        addMetric(metrics, 1, text("dashboard.nodes"), dashboard.nodesProperty().asString());
        addMetric(metrics, 2, text("dashboard.edges"), dashboard.edgesProperty().asString());
        addMetric(metrics, 3, text("dashboard.problems"), dashboard.problemsProperty().asString());
        addMetric(metrics, 4, text("dashboard.tasks"), dashboard.tasksProperty().asString());

        ArchitectureDiagramView architectureDiagram = new ArchitectureDiagramView();
        architectureDiagram.setArchitecture(dashboard.architectureProperty().get());
        dashboard.architectureProperty().addListener((obs, old, value) -> architectureDiagram.setArchitecture(value));

        TextArea c4Source = new TextArea();
        c4Source.setEditable(false);
        c4Source.textProperty().bind(dashboard.c4SourceProperty());
        c4Source.getStyleClass().add("c4-source");

        TabPane architectureTabs = new TabPane(
                new Tab(text("architecture.diagram"), architectureDiagram),
                new Tab(text("architecture.source"), c4Source)
        );
        architectureTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(architectureTabs, Priority.ALWAYS);

        Label projectName = new Label();
        projectName.textProperty().bind(dashboard.projectNameProperty());
        projectName.getStyleClass().add("project-name");

        VBox content = new VBox(12, projectName, metrics, architectureTabs);
        content.setPadding(new Insets(16));
        VBox.setVgrow(content, Priority.ALWAYS);

        Tab tab = new Tab(text("tab.overview"), content);
        return tab;
    }

    private Tab graphTab() {
        GraphCanvas canvas = new GraphCanvas();
        TextArea details = new TextArea();
        details.setEditable(false);
        details.setWrapText(true);
        details.getStyleClass().add("graph-details");

        TextField search = new TextField();
        search.setPromptText(text("graph.search"));
        ComboBox<GraphMode> mode = new ComboBox<>();
        mode.getItems().setAll(GraphMode.values());
        mode.getSelectionModel().select(GraphMode.ARCHITECTURE);
        ComboBox<GraphProblemFilter> problemFilter = new ComboBox<>();
        problemFilter.getItems().setAll(GraphProblemFilter.values());
        problemFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(GraphProblemFilter value) {
                return value == null ? "" : text("graph.problemFilter." + value.name().toLowerCase(Locale.ROOT));
            }

            @Override
            public GraphProblemFilter fromString(String value) {
                return GraphProblemFilter.ALL;
            }
        });
        problemFilter.getSelectionModel().select(GraphProblemFilter.ALL);

        CheckBox methods = new CheckBox(text("graph.showMethods"));
        CheckBox imports = new CheckBox(text("graph.showImports"));
        CheckBox external = new CheckBox(text("graph.showExternal"));
        external.setSelected(true);
        CheckBox projectOnly = new CheckBox(text("graph.projectOnly"));
        projectOnly.setSelected(true);

        Label summary = new Label(text("graph.empty"));
        summary.getStyleClass().add("graph-summary");

        final String[] focusedNodeId = {""};
        Runnable refresh = () -> {
            ProjectSession session = viewModel.sessionProperty().get();
            if (session == null) {
                canvas.setProjection(GraphProjection.empty());
                details.setText(text("error.noProject"));
                summary.setText(text("graph.empty"));
                return;
            }
            GraphProjectionOptions options = new GraphProjectionOptions(
                    mode.getSelectionModel().getSelectedItem(),
                    methods.isSelected(),
                    imports.isSelected(),
                    external.isSelected(),
                    projectOnly.isSelected(),
                    problemFilter.getSelectionModel().getSelectedItem(),
                    search.getText(),
                    focusedNodeId[0],
                    350
            );
            GraphProjection projection = graphProjectionService.project(session.graph(), session.problems(), options);
            canvas.setProjection(projection);
            summary.setText(graphSummary(projection));
            if (projection.truncated()) {
                status.setText(text("graph.truncated"));
            }
        };

        Button reset = new Button(text("graph.reset"));
        reset.setOnAction(event -> {
            focusedNodeId[0] = "";
            search.clear();
            mode.getSelectionModel().select(GraphMode.ARCHITECTURE);
            methods.setSelected(false);
            imports.setSelected(false);
            external.setSelected(true);
            projectOnly.setSelected(true);
            problemFilter.getSelectionModel().select(GraphProblemFilter.ALL);
            canvas.resetView();
            refresh.run();
        });

        Button focus = new Button(text("graph.focus"));
        focus.setOnAction(event -> {
            GraphVertex selected = canvas.selectedVertexProperty().get();
            if (selected != null) {
                focusedNodeId[0] = selected.id();
                mode.getSelectionModel().select(GraphMode.IMPACT);
                refresh.run();
            }
        });

        search.textProperty().addListener((obs, old, value) -> {
            focusedNodeId[0] = "";
            refresh.run();
        });
        mode.valueProperty().addListener((obs, old, value) -> refresh.run());
        methods.selectedProperty().addListener((obs, old, value) -> refresh.run());
        imports.selectedProperty().addListener((obs, old, value) -> refresh.run());
        external.selectedProperty().addListener((obs, old, value) -> refresh.run());
        projectOnly.selectedProperty().addListener((obs, old, value) -> refresh.run());
        problemFilter.valueProperty().addListener((obs, old, value) -> refresh.run());
        viewModel.sessionProperty().addListener((obs, old, value) -> refresh.run());
        canvas.selectedVertexProperty().addListener((obs, old, selected) ->
                details.setText(selected == null ? text("graph.noSelection") : vertexDetails(selected))
        );

        Label problemFilterLabel = new Label(text("graph.problemFilter"));
        HBox filters = new HBox(8, search, mode, problemFilterLabel, problemFilter, methods, imports, external, projectOnly, new Separator(), focus, reset, summary);
        filters.getStyleClass().add("graph-toolbar");
        filters.setPadding(new Insets(10));
        HBox.setHgrow(search, Priority.ALWAYS);

        VBox sidePanel = new VBox(8, new Label(text("graph.selection")), details);
        sidePanel.getStyleClass().add("graph-side-panel");
        sidePanel.setPadding(new Insets(12));
        sidePanel.setPrefWidth(320);
        VBox.setVgrow(details, Priority.ALWAYS);

        SplitPane split = new SplitPane(canvas, sidePanel);
        split.setDividerPositions(0.75);

        BorderPane content = new BorderPane(split);
        content.setTop(filters);
        refresh.run();
        return new Tab(text("tab.graph"), content);
    }

    private Tab problemsTab() {
        ListView<DetectedProblem> list = new ListView<>(viewModel.problems());
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(DetectedProblem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "[" + item.severity() + "] " + item.title());
            }
        });
        TextArea details = new TextArea();
        details.setEditable(false);
        details.setWrapText(true);
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected == null) {
                details.clear();
                return;
            }
            details.setText(String.join(
                    "\n",
                    selected.description(),
                    "",
                    text("label.evidence") + ":",
                    String.join("\n", selected.evidence()),
                    "",
                    selected.primaryPath() + ":" + selected.line()
            ));
        });
        VBox.setVgrow(list, Priority.ALWAYS);
        VBox.setVgrow(details, Priority.SOMETIMES);
        VBox content = new VBox(8, list, details);
        content.setPadding(new Insets(12));
        return new Tab(text("tab.problems"), content);
    }

    private Tab tasksTab() {
        ListView<EngineeringTask> list = new ListView<>(viewModel.tasks());
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(EngineeringTask item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "[" + item.risk() + "] " + item.title());
            }
        });
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) ->
                viewModel.selectedTaskProperty().set(selected)
        );
        viewModel.selectedTaskProperty().addListener((obs, old, selected) -> {
            if (selected != list.getSelectionModel().getSelectedItem()) {
                list.getSelectionModel().select(selected);
            }
        });

        TextArea details = new TextArea();
        details.setEditable(false);
        details.setWrapText(true);
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected == null) {
                details.clear();
                return;
            }
            details.setText(String.join(
                    "\n",
                    selected.goal(),
                    "",
                    text("label.plan") + ":",
                    String.join("\n", selected.suggestedPlan()),
                    "",
                    text("label.definitionOfDone") + ": " + selected.definitionOfDone()
            ));
        });

        Button plan = new Button(text("action.planAgent"));
        plan.setOnAction(event -> runBackground(text("action.planAgent"), () -> {
            ProjectSession session = requireSession();
            EngineeringTask task = viewModel.selectedTaskProperty().get();
            if (task == null) {
                throw new IllegalStateException(text("error.noTaskSelected"));
            }
            var run = facade.startAgentRun(session, task);
            Platform.runLater(() -> viewModel.applyAgentRun(run));
        }));

        Button approve = new Button(text("action.approvePlan"));
        approve.setOnAction(event -> runBackground(text("action.approvePlan"), () -> {
            ProjectSession session = requireSession();
            var run = viewModel.activeRunProperty().get();
            if (run == null) {
                throw new IllegalStateException(text("error.noActiveRun"));
            }
            var approved = facade.approveAgentRun(session, run.id());
            var diff = facade.workspaceDiff(session);
            var messages = facade.conversation(session);
            Platform.runLater(() -> {
                viewModel.applyAgentRun(approved);
                viewModel.applyDiff(diff);
                viewModel.setChatHistory(messages);
            });
        }));

        Button verify = new Button(text("action.runTests"));
        verify.setOnAction(event -> runBackground(text("action.runTests"), () -> {
            ProjectSession session = requireSession();
            EngineeringTask task = viewModel.selectedTaskProperty().get();
            if (task == null) {
                throw new IllegalStateException(text("error.noTaskSelected"));
            }
            var result = facade.verifyTask(session, task);
            var diff = facade.workspaceDiff(session);
            Platform.runLater(() -> {
                viewModel.applyVerification(result);
                viewModel.applyDiff(diff);
            });
        }));

        Button accept = new Button(text("action.acceptChanges"));
        accept.setOnAction(event -> runBackground(text("action.acceptChanges"), () -> {
            ProjectSession session = requireSession();
            var run = viewModel.activeRunProperty().get();
            if (run == null) {
                throw new IllegalStateException(text("error.noActiveRun"));
            }
            var accepted = facade.acceptAgentRun(session, run.id());
            Platform.runLater(() -> viewModel.applyAgentRun(accepted));
        }));

        Button rollback = new Button(text("action.rollback"));
        rollback.setOnAction(event -> runBackground(text("action.rollback"), () -> {
            ProjectSession session = requireSession();
            var run = viewModel.activeRunProperty().get();
            if (run == null) {
                throw new IllegalStateException(text("error.noActiveRun"));
            }
            var rolledBack = facade.rollbackAgentRun(session, run.id());
            var diff = facade.workspaceDiff(session);
            Platform.runLater(() -> {
                viewModel.applyAgentRun(rolledBack);
                viewModel.applyDiff(diff);
            });
        }));

        HBox actions = new HBox(8, plan, approve, verify, accept, rollback);
        VBox.setVgrow(list, Priority.ALWAYS);
        VBox content = new VBox(8, list, details, actions);
        content.setPadding(new Insets(12));
        return new Tab(text("tab.tasks"), content);
    }

    private Tab agentRunTab() {
        ListView<String> events = new ListView<>(viewModel.agentEventLines());
        VBox.setVgrow(events, Priority.ALWAYS);

        Label runtime = new Label();
        runtime.textProperty().bind(viewModel.runtimeStatusProperty());
        runtime.getStyleClass().add("runtime-status");

        Button step = new Button(text("action.executeStep"));
        step.setOnAction(event -> runBackground(text("action.executeStep"), () -> {
            ProjectSession session = requireSession();
            var run = viewModel.activeRunProperty().get();
            if (run == null) {
                throw new IllegalStateException(text("error.noActiveRun"));
            }
            var updated = facade.executeAgentStep(session, run.id());
            var diff = facade.workspaceDiff(session);
            Platform.runLater(() -> {
                viewModel.applyAgentRun(updated);
                viewModel.applyDiff(diff);
            });
        }));

        HBox actions = new HBox(8, step);
        VBox content = new VBox(8, runtime, events, actions);
        content.setPadding(new Insets(12));
        return new Tab(text("tab.agentRun"), content);
    }

    private Tab chatTab() {
        ListView<String> history = new ListView<>(viewModel.chatLines());
        VBox.setVgrow(history, Priority.ALWAYS);

        TextField input = new TextField();
        input.setPromptText(text("chat.prompt"));
        Button ask = new Button(text("action.askGraph"));
        ask.setOnAction(event -> {
            String question = input.getText();
            if (question == null || question.isBlank()) {
                return;
            }
            input.clear();
            runBackground(text("action.askGraph"), () -> {
                ProjectSession session = requireSession();
                var answer = facade.askGraph(session, question);
                var messages = facade.conversation(session);
                Platform.runLater(() -> {
                    viewModel.applyGraphAnswer(answer);
                    viewModel.setChatHistory(messages);
                });
            });
        });

        Button directTask = new Button(text("action.startDirectTask"));
        directTask.setOnAction(event -> {
            String taskText = input.getText();
            if (taskText == null || taskText.isBlank()) {
                return;
            }
            input.clear();
            runBackground(text("action.startDirectTask"), () -> {
                ProjectSession session = requireSession();
                var run = facade.submitDirectTask(session, taskText);
                var messages = facade.conversation(session);
                Platform.runLater(() -> {
                    viewModel.applyAgentRun(run);
                    viewModel.setChatHistory(messages);
                });
            });
        });

        HBox composer = new HBox(8, input, ask, directTask);
        HBox.setHgrow(input, Priority.ALWAYS);
        VBox content = new VBox(8, history, composer);
        content.setPadding(new Insets(12));
        return new Tab(text("tab.chat"), content);
    }

    private Tab diffTab() {
        TextArea diff = new TextArea();
        diff.setEditable(false);
        diff.setWrapText(true);
        diff.textProperty().bind(viewModel.diffTextProperty());
        VBox content = new VBox(diff);
        VBox.setVgrow(diff, Priority.ALWAYS);
        content.setPadding(new Insets(12));
        return new Tab(text("tab.diff"), content);
    }

    private Tab terminalTab() {
        TextArea terminal = new TextArea();
        terminal.setEditable(false);
        terminal.setWrapText(true);
        terminal.textProperty().bind(viewModel.terminalOutputProperty());
        VBox content = new VBox(terminal);
        VBox.setVgrow(terminal, Priority.ALWAYS);
        content.setPadding(new Insets(12));
        return new Tab(text("tab.terminal"), content);
    }

    private void addMetric(GridPane grid, int column, String label, javafx.beans.value.ObservableValue<String> value) {
        Label valueLabel = new Label();
        valueLabel.textProperty().bind(value);
        valueLabel.getStyleClass().add("metric-value");

        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("metric-label");

        VBox box = new VBox(4, valueLabel, nameLabel);
        box.getStyleClass().add("metric");
        grid.add(box, column, 0);
    }

    private String graphSummary(GraphProjection projection) {
        if (projection.vertices().isEmpty()) {
            return text("graph.empty");
        }
        String suffix = projection.truncated() ? " / " + projection.availableNodes() : "";
        return projection.vertices().size() + suffix + " " + text("graph.nodes")
                + ", " + projection.links().size() + " " + text("graph.links");
    }

    private String vertexDetails(GraphVertex vertex) {
        String problems = vertex.problems().isEmpty()
                ? "-"
                : String.join("\n", vertex.problems());
        return String.join(
                "\n",
                vertex.qualifiedName(),
                "",
                text("graph.kind") + ": " + vertex.kind(),
                text("graph.language") + ": " + vertex.language(),
                text("graph.path") + ": " + vertex.filePath() + ":" + vertex.line(),
                text("graph.problems") + ": " + vertex.problemCount(),
                problems,
                "",
                text("graph.signature") + ":",
                vertex.signature().isBlank() ? "-" : vertex.signature()
        );
    }

    private void chooseAndScanProject() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(text("action.openProject"));
        File selected = chooser.showDialog(root.getScene().getWindow());
        if (selected == null) {
            return;
        }
        openProject(selected.toPath());
    }

    public void openProject(Path projectRoot) {
        runBackground(text("action.scan"), () -> {
            ProjectSession session = facade.openAndScan(projectRoot, progress -> Platform.runLater(() ->
                    status.setText(text("action.scan") + ": " + progress.phase()
                            + " " + progress.current() + "/" + progress.total()
                            + currentFileSuffix(progress.currentFile()))
            ));
            var gitStatus = facade.gitStatus(session);
            var messages = facade.conversation(session);
            Platform.runLater(() -> {
                viewModel.applySession(session);
                viewModel.applyGitBranch(gitStatus.branch());
                viewModel.setChatHistory(messages);
                status.setText(localization.message(locale, "status.projectOpened", session.project().displayName()));
            });
        });
    }

    private String currentFileSuffix(String currentFile) {
        return currentFile == null || currentFile.isBlank() ? "" : " " + currentFile;
    }

    private ProjectSession requireSession() {
        ProjectSession session = viewModel.sessionProperty().get();
        if (session == null) {
            throw new IllegalStateException(text("error.noProject"));
        }
        return session;
    }

    private void runBackground(String actionLabel, Runnable work) {
        status.setText(actionLabel);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                work.run();
                return null;
            }
        };
        task.setOnFailed(event -> Platform.runLater(() ->
                status.setText(localization.message(locale, "status.error", task.getException().getMessage()))
        ));
        task.setOnSucceeded(event -> Platform.runLater(() -> status.setText(text("status.ready"))));
        Thread.ofVirtual().name("chatly-ui-", 0).start(task);
    }

    private String text(String key) {
        return localization.message(locale, key);
    }

    private void applyLlmStatus() {
        var status = facade.llmStatus();
        viewModel.applyLlmStatus(status.configured(), status.profile().provider(), status.profile().model());
    }
}
