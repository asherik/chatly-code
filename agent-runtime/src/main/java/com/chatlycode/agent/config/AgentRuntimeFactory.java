package com.chatlycode.agent.config;

import com.chatlycode.agent.application.AgentActionPlanner;
import com.chatlycode.agent.application.AgentContextBuilder;
import com.chatlycode.agent.application.AgentLoop;
import com.chatlycode.agent.application.AgentOrchestrator;
import com.chatlycode.agent.application.AgentPlanner;
import com.chatlycode.agent.event.AgentEventStore;
import com.chatlycode.agent.policy.CommandPolicyService;
import com.chatlycode.agent.tool.AgentToolRegistry;
import com.chatlycode.agent.tool.ApplyPatchTool;
import com.chatlycode.agent.tool.GitDiffTool;
import com.chatlycode.agent.tool.GitStatusTool;
import com.chatlycode.agent.tool.GlobTool;
import com.chatlycode.agent.tool.GraphQueryTool;
import com.chatlycode.agent.tool.GrepTool;
import com.chatlycode.agent.tool.ListFilesTool;
import com.chatlycode.agent.tool.ReadFileTool;
import com.chatlycode.agent.tool.RunCommandTool;
import com.chatlycode.conversation.application.ConversationService;
import com.chatlycode.git.application.GitService;
import com.chatlycode.llm.application.LlmGateway;
import com.chatlycode.runtime.application.RuntimeService;
import com.chatlycode.shared.time.ClockProvider;
import com.chatlycode.workspace.application.WorkspaceSafetyService;

public final class AgentRuntimeFactory {

    private AgentRuntimeFactory() {
    }

    public static AgentOrchestrator create(
            WorkspaceSafetyService workspaceSafetyService,
            GitService gitService,
            RuntimeService runtimeService,
            ConversationService conversationService,
            LlmGateway llmGateway,
            ClockProvider clock
    ) {
        CommandPolicyService commandPolicyService = new CommandPolicyService();
        AgentToolRegistry toolRegistry = new AgentToolRegistry()
                .register(new ReadFileTool(workspaceSafetyService))
                .register(new ListFilesTool(workspaceSafetyService))
                .register(new GrepTool())
                .register(new GlobTool())
                .register(new RunCommandTool(runtimeService, commandPolicyService))
                .register(new ApplyPatchTool(workspaceSafetyService))
                .register(new GraphQueryTool())
                .register(new GitStatusTool(gitService))
                .register(new GitDiffTool(gitService));

        AgentEventStore eventStore = new AgentEventStore();
        AgentLoop agentLoop = new AgentLoop(toolRegistry, eventStore, conversationService, clock);

        return new AgentOrchestrator(
                new AgentPlanner(),
                new AgentActionPlanner(),
                new AgentContextBuilder(),
                agentLoop,
                eventStore,
                gitService,
                runtimeService,
                conversationService,
                llmGateway,
                clock
        );
    }
}
