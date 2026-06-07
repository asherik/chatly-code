package com.chatlycode.desktop.controller;

import com.chatlycode.appserver.facade.ChatlyCodeFacade;
import com.chatlycode.desktop.viewmodel.DashboardViewModel;
import com.chatlycode.i18n.LocalizationService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
    private final DashboardViewModel viewModel = new DashboardViewModel();
    private final BorderPane root = new BorderPane();
    private final Label status = new Label();

    public MainController(ChatlyCodeFacade facade, LocalizationService localization, Locale locale) {
        this.facade = facade;
        this.localization = localization;
        this.locale = locale == null ? Locale.ENGLISH : locale;
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

        HBox toolbar = new HBox(12, title, openProject);
        toolbar.getStyleClass().add("toolbar");
        toolbar.setPadding(new Insets(12));

        GridPane metrics = new GridPane();
        metrics.getStyleClass().add("metrics");
        metrics.setHgap(12);
        metrics.setVgap(12);
        addMetric(metrics, 0, text("dashboard.files"), viewModel.filesProperty().asString());
        addMetric(metrics, 1, text("dashboard.nodes"), viewModel.nodesProperty().asString());
        addMetric(metrics, 2, text("dashboard.edges"), viewModel.edgesProperty().asString());
        addMetric(metrics, 3, text("dashboard.problems"), viewModel.problemsProperty().asString());
        addMetric(metrics, 4, text("dashboard.tasks"), viewModel.tasksProperty().asString());

        TextArea mermaid = new TextArea();
        mermaid.setEditable(false);
        mermaid.textProperty().bind(viewModel.mermaidProperty());
        VBox.setVgrow(mermaid, Priority.ALWAYS);

        Label projectName = new Label();
        projectName.textProperty().bind(viewModel.projectNameProperty());
        projectName.getStyleClass().add("project-name");

        VBox center = new VBox(12, projectName, metrics, mermaid);
        center.setPadding(new Insets(16));
        VBox.setVgrow(center, Priority.ALWAYS);

        status.setText(text("status.ready"));
        status.getStyleClass().add("status");

        root.setTop(toolbar);
        root.setCenter(center);
        root.setBottom(status);
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

        status.setText(text("action.scan"));
        Task<Void> scanTask = new Task<>() {
            @Override
            protected Void call() {
                var snapshot = facade.openAndScan(selected.toPath());
                Platform.runLater(() -> {
                    viewModel.apply(snapshot);
                    status.setText(localization.message(locale, "status.projectOpened", snapshot.project().displayName()));
                });
                return null;
            }
        };
        scanTask.setOnFailed(event -> status.setText(scanTask.getException().getMessage()));
        Thread.ofVirtual().name("project-scan-", 0).start(scanTask);
    }

    private String text(String key) {
        return localization.message(locale, key);
    }
}
