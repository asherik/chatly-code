package com.chatlycode.project.port;

import com.chatlycode.project.domain.OpenedProject;
import com.chatlycode.project.domain.ProjectId;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {

    OpenedProject save(OpenedProject project);

    Optional<OpenedProject> findById(ProjectId id);

    List<OpenedProject> findAll();
}
