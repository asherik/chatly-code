package com.chatlycode.desktop.viewmodel;

import com.chatlycode.agent.domain.AgentEvent;
import com.chatlycode.agent.domain.AgentObservation;
import com.chatlycode.agent.domain.AgentRun;
import com.chatlycode.appserver.facade.ProjectSession;
import com.chatlycode.conversation.domain.ConversationMessage;
import com.chatlycode.graph.query.GraphAnswer;
import com.chatlycode.problem.domain.DetectedProblem;
import com.chatlycode.runtime.domain.CommandResult;
import com.chatlycode.task.domain.EngineeringTask;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class MainViewModel {

    private final DashboardViewModel dashboard = new DashboardViewModel();
    private final ObservableList<DetectedProblem> problems = FXCollections.observableArrayList();
    private final ObservableList<EngineeringTask> tasks = FXCollections.observableArrayList();
    private final ObservableList<String> chatLines = FXCollections.observableArrayList();
    private final ObservableList<String> agentEventLines = FXCollections.observableArrayList();
    private final ObjectProperty<ProjectSession> session = new SimpleObjectProperty<>();
    private final ObjectProperty<EngineeringTask> selectedTask = new SimpleObjectProperty<>();
    private final ObjectProperty<AgentRun> activeRun = new SimpleObjectProperty<>();
    private final StringProperty diffText = new SimpleStringProperty("");
    private final StringProperty terminalOutput = new SimpleStringProperty("");
    private final StringProperty gitBranch = new SimpleStringProperty("");
    private final StringProperty llmStatus = new SimpleStringProperty("");
    private final StringProperty lastGraphAnswer = new SimpleStringProperty("");
    private final StringProperty runtimeStatus = new SimpleStringProperty("");

    public void applySession(ProjectSession projectSession) {
        session.set(projectSession);
        dashboard.apply(projectSession.toDashboard());
        problems.setAll(projectSession.problems());
        tasks.setAll(projectSession.tasks());
        diffText.set("");
        terminalOutput.set("");
        activeRun.set(null);
        selectedTask.set(tasks.isEmpty() ? null : tasks.getFirst());
        chatLines.clear();
        agentEventLines.clear();
        runtimeStatus.set("");
    }

    public void appendChat(ConversationMessage message) {
        chatLines.add(message.author() + ": " + message.content());
    }

    public void setChatHistory(java.util.List<ConversationMessage> messages) {
        chatLines.clear();
        messages.forEach(this::appendChat);
    }

    public void applyGraphAnswer(GraphAnswer answer) {
        lastGraphAnswer.set(answer.summary());
        chatLines.add("GRAPH: " + answer.summary());
        if (!answer.evidence().isEmpty()) {
            chatLines.add("EVIDENCE: " + String.join("; ", answer.evidence()));
        }
    }

    public void applyVerification(CommandResult result) {
        terminalOutput.set(formatCommandResult(result));
    }

    public void applyDiff(String diff) {
        diffText.set(diff == null || diff.isBlank() ? "" : diff);
    }

    public void applyGitBranch(String branch) {
        gitBranch.set(branch == null ? "" : branch);
    }

    public void applyLlmStatus(boolean configured, String provider, String model) {
        String prefix = configured ? "LLM" : "LLM offline";
        llmStatus.set(prefix + ": " + provider + "/" + model);
    }

    public void applyAgentRun(AgentRun run) {
        activeRun.set(run);
        runtimeStatus.set(run.runtimeMode() + " / " + run.status());
        chatLines.add("AGENT: " + run.status() + " (" + run.actions().size() + " actions)");
        agentEventLines.clear();
        run.events().forEach(event -> agentEventLines.add(formatEvent(event)));
        run.observations().forEach(this::appendObservation);
    }

    public void applyAgentEvents(java.util.List<AgentEvent> events) {
        agentEventLines.clear();
        events.forEach(event -> agentEventLines.add(formatEvent(event)));
    }

    private void appendObservation(AgentObservation observation) {
        agentEventLines.add("OBSERVATION: " + observation.summary());
        if (observation.detail() != null && !observation.detail().isBlank()) {
            terminalOutput.set(observation.detail());
        }
    }

    private String formatEvent(AgentEvent event) {
        return event.type() + " [" + event.actor() + "]: " + event.message();
    }

    private String formatCommandResult(CommandResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("exit=").append(result.exitCode()).append('\n');
        if (!result.stdout().isBlank()) {
            builder.append(result.stdout());
        }
        if (!result.stderr().isBlank()) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(result.stderr());
        }
        return builder.toString();
    }

    public DashboardViewModel dashboard() {
        return dashboard;
    }

    public ObservableList<DetectedProblem> problems() {
        return problems;
    }

    public ObservableList<EngineeringTask> tasks() {
        return tasks;
    }

    public ObservableList<String> chatLines() {
        return chatLines;
    }

    public ObjectProperty<ProjectSession> sessionProperty() {
        return session;
    }

    public ObjectProperty<EngineeringTask> selectedTaskProperty() {
        return selectedTask;
    }

    public ObjectProperty<AgentRun> activeRunProperty() {
        return activeRun;
    }

    public StringProperty diffTextProperty() {
        return diffText;
    }

    public StringProperty terminalOutputProperty() {
        return terminalOutput;
    }

    public StringProperty gitBranchProperty() {
        return gitBranch;
    }

    public StringProperty llmStatusProperty() {
        return llmStatus;
    }

    public StringProperty lastGraphAnswerProperty() {
        return lastGraphAnswer;
    }

    public ObservableList<String> agentEventLines() {
        return agentEventLines;
    }

    public StringProperty runtimeStatusProperty() {
        return runtimeStatus;
    }
}
