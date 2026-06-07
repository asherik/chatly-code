package com.chatlycode.desktop.controller;

import com.chatlycode.appserver.facade.ChatlyCodeFacade;
import com.chatlycode.appserver.facade.ProjectSession;
import com.chatlycode.desktop.viewmodel.MainViewModel;
import com.chatlycode.i18n.LocalizationService;
import com.chatlycode.problem.domain.DetectedProblem;
import com.chatlycode.task.domain.EngineeringTask;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.Locale;

public final class MainController {

    private final ChatlyCodeFacade facade;
    private final LocalizationService localization;
    private final Locale locale;
    private final MainViewModel viewModel = new MainViewModel();
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

        TextArea mermaid = new TextArea();
        mermaid.setEditable(false);
        mermaid.textProperty().bind(dashboard.mermaidProperty());
        VBox.setVgrow(mermaid, Priority.ALWAYS);

        Label projectName = new Label();
        projectName.textProperty().bind(dashboard.projectNameProperty());
        projectName.getStyleClass().add("project-name");

        VBox content = new VBox(12, projectName, metrics, mermaid);
        content.setPadding(new Insets(16));
        VBox.setVgrow(content, Priority.ALWAYS);

        Tab tab = new Tab(text("tab.overview"), content);
        return tab;
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

    private void chooseAndScanProject() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(text("action.openProject"));
        File selected = chooser.showDialog(root.getScene().getWindow());
        if (selected == null) {
            return;
        }
        runBackground(text("action.scan"), () -> {
            ProjectSession session = facade.openAndScan(selected.toPath());
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
