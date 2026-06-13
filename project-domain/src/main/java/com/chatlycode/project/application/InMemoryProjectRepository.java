package com.chatlycode.project.application;

import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.project.domain.ProjectId;
import com.chatlycode.project.port.ProjectRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryProjectRepository implements ProjectRepository {

    private final ConcurrentMap<ProjectId, OpenedProject> projects = new ConcurrentHashMap<>();

    @Override
    public OpenedProject save(OpenedProject project) {
        projects.put(project.id(), project);
        return project;
    }

    @Override
    public Optional<OpenedProject> findById(ProjectId id) {
        return Optional.ofNullable(projects.get(id));
    }

    @Override
    public List<OpenedProject> findAll() {
        return List.copyOf(new ArrayList<>(projects.values()));
    }
}
