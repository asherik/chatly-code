package com.chatlycode.agent.application;

import com.chatlycode.agent.domain.AgentPlan;
import com.chatlycode.task.domain.EngineeringTask;

import java.util.List;

public final class AgentPlanner {

    public AgentPlan plan(EngineeringTask task) {
        return new AgentPlan(
                task.id(),
                List.of(
                        "Inspect linked evidence and files",
                        "Prepare a minimal workspace patch",
                        "Run allowed verification commands",
                        "Present diff for review"
                ),
                "Workspace writes must go through WorkspaceSafety and remain inside the project root."
        );
    }
}
