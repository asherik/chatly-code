package com.chatlycode.desktop.viewmodel;

import com.chatlycode.appserver.facade.DashboardSnapshot;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class DashboardViewModel {

    private final StringProperty projectName = new SimpleStringProperty("");
    private final IntegerProperty files = new SimpleIntegerProperty();
    private final IntegerProperty nodes = new SimpleIntegerProperty();
    private final IntegerProperty edges = new SimpleIntegerProperty();
    private final IntegerProperty problems = new SimpleIntegerProperty();
    private final IntegerProperty tasks = new SimpleIntegerProperty();
    private final StringProperty mermaid = new SimpleStringProperty("");

    public void apply(DashboardSnapshot snapshot) {
        projectName.set(snapshot.project().displayName());
        files.set(snapshot.architecture().fileCount());
        nodes.set(snapshot.architecture().nodeCount());
        edges.set(snapshot.architecture().edgeCount());
        problems.set(snapshot.problems().size());
        tasks.set(snapshot.tasks().size());
        mermaid.set(snapshot.architecture().mermaidC4Draft());
    }

    public StringProperty projectNameProperty() {
        return projectName;
    }

    public IntegerProperty filesProperty() {
        return files;
    }

    public IntegerProperty nodesProperty() {
        return nodes;
    }

    public IntegerProperty edgesProperty() {
        return edges;
    }

    public IntegerProperty problemsProperty() {
        return problems;
    }

    public IntegerProperty tasksProperty() {
        return tasks;
    }

    public StringProperty mermaidProperty() {
        return mermaid;
    }
}
