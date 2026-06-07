package com.chatlycode.git.application;

import com.chatlycode.git.domain.GitStatus;
import com.chatlycode.workspace.domain.WorkspaceRoot;

public interface GitService {

    GitStatus status(WorkspaceRoot root);

    String diff(WorkspaceRoot root);
}
