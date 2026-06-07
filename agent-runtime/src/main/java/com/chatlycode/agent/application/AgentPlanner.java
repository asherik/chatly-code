package com.chatlycode.agent.application;

import com.chatlycode.agent.domain.AgentPlan;
import com.chatlycode.problem.domain.DetectedProblem;
import com.chatlycode.task.domain.EngineeringTask;

import java.util.ArrayList;
import java.util.List;

public final class AgentPlanner {

    public AgentPlan plan(EngineeringTask task, List<DetectedProblem> linkedProblems) {
        List<String> steps = new ArrayList<>();
        steps.add("Inspect linked evidence and affected files");
        if (!task.suggestedPlan().isEmpty()) {
            steps.addAll(task.suggestedPlan());
        } else {
            steps.add("Prepare a minimal workspace patch");
            steps.add("Run allowed verification commands");
            steps.add("Present diff for review");
        }
        String riskNote = linkedProblems.isEmpty()
                ? "Workspace writes must go through WorkspaceSafety and remain inside the project root."
                : "Primary problem: " + linkedProblems.getFirst().title() + ". Risk: " + task.risk();
        return new AgentPlan(task.id(), List.copyOf(steps), riskNote);
    }
}
