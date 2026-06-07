package com.chatlycode.agent.application;

import com.chatlycode.agent.domain.AgentAction;
import com.chatlycode.agent.domain.AgentActionType;
import com.chatlycode.agent.domain.ApprovalState;
import com.chatlycode.agent.domain.ToolResultStatus;
import com.chatlycode.graph.domain.CodeGraph;
import com.chatlycode.problem.domain.DetectedProblem;
import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.shared.id.Ids;
import com.chatlycode.task.domain.EngineeringTask;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AgentActionPlanner {

    public List<AgentAction> planForTask(
            EngineeringTask task,
            OpenedProject project,
            CodeGraph graph,
            List<DetectedProblem> problems
    ) {
        List<AgentAction> actions = new ArrayList<>();
        actions.add(action(AgentActionType.GIT_STATUS, "Inspect git status", Map.of()));
        actions.add(action(AgentActionType.GRAPH_QUERY, "Inspect graph context", Map.of(
                "question", task.goal().isBlank() ? task.title() : task.goal()
        )));

        for (var file : task.affectedFiles()) {
            actions.add(action(AgentActionType.READ_FILE, "Read " + file, Map.of("path", file.toString().replace('\\', '/'))));
        }

        if (!task.affectedFiles().isEmpty()) {
            String path = task.affectedFiles().getFirst().toString().replace('\\', '/');
            actions.add(action(AgentActionType.GREP, "Search related symbols", Map.of(
                    "query", extractSymbol(path)
            )));
        }

        if (!task.testCommand().isEmpty()) {
            actions.add(action(
                    AgentActionType.RUN_COMMAND,
                    "Run verification command",
                    Map.of("command", String.join(" ", task.testCommand())),
                    ApprovalState.PENDING
            ));
        } else if (!project.buildProfile().testCommand().isEmpty()) {
            actions.add(action(
                    AgentActionType.RUN_COMMAND,
                    "Run project test command",
                    Map.of("command", String.join(" ", project.buildProfile().testCommand())),
                    ApprovalState.PENDING
            ));
        }

        actions.add(action(AgentActionType.GIT_DIFF, "Collect workspace diff", Map.of()));
        return List.copyOf(actions);
    }

    public List<AgentAction> planForDirectTask(String directTask, OpenedProject project, CodeGraph graph) {
        List<AgentAction> actions = new ArrayList<>();
        actions.add(action(AgentActionType.GIT_STATUS, "Inspect git status", Map.of()));
        actions.add(action(AgentActionType.LIST_FILES, "List workspace files", Map.of()));
        actions.add(action(AgentActionType.GRAPH_QUERY, "Query code graph", Map.of("question", directTask)));
        actions.add(action(AgentActionType.GLOB, "Find likely source files", Map.of("pattern", "**/*.java")));

        graph.nodes().stream()
                .filter(node -> node.qualifiedName().toLowerCase().contains(extractToken(directTask)))
                .limit(3)
                .forEach(node -> actions.add(action(
                        AgentActionType.READ_FILE,
                        "Read " + node.filePath(),
                        Map.of("path", node.filePath().toString().replace('\\', '/'))
                )));

        if (!project.buildProfile().testCommand().isEmpty()) {
            actions.add(action(
                    AgentActionType.RUN_COMMAND,
                    "Run project tests",
                    Map.of("command", String.join(" ", project.buildProfile().testCommand())),
                    ApprovalState.PENDING
            ));
        }
        actions.add(action(AgentActionType.GIT_DIFF, "Collect workspace diff", Map.of()));
        return List.copyOf(actions);
    }

    private AgentAction action(AgentActionType type, String summary, Map<String, String> arguments) {
        return action(type, summary, arguments, ApprovalState.NOT_REQUIRED);
    }

    private AgentAction action(AgentActionType type, String summary, Map<String, String> arguments, ApprovalState approvalState) {
        return new AgentAction(
                Ids.newId("action"),
                type,
                summary,
                arguments,
                approvalState,
                ToolResultStatus.SKIPPED,
                null,
                null
        );
    }

    private String extractSymbol(String path) {
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        return fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
    }

    private String extractToken(String text) {
        String[] parts = text.toLowerCase().split("\\s+");
        for (String part : parts) {
            if (part.length() >= 4) {
                return part;
            }
        }
        return "service";
    }
}
