package com.chatlycode.agent.tool;

import com.chatlycode.agent.domain.AgentActionType;
import com.chatlycode.graph.query.GraphQueryService;

import java.util.Map;
import java.util.stream.Collectors;

public final class GraphQueryTool implements AgentTool {

    private final GraphQueryService graphQueryService = new GraphQueryService();

    @Override
    public AgentActionType type() {
        return AgentActionType.GRAPH_QUERY;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, String> arguments) {
        String question = arguments.getOrDefault("question", arguments.get("query"));
        if (question == null || question.isBlank()) {
            return AgentToolResult.failed("Missing question", "question argument is required");
        }
        var answer = graphQueryService.answerQuestion(context.graph(), question);
        String detail = String.join(
                "\n",
                answer.summary(),
                "Evidence: " + String.join("; ", answer.evidence()),
                "Files: " + answer.relatedFiles().stream().collect(Collectors.joining(", "))
        );
        return AgentToolResult.success("Graph query answered", detail);
    }
}
