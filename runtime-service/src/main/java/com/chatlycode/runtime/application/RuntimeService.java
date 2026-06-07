package com.chatlycode.runtime.application;

import com.chatlycode.runtime.domain.CommandRequest;
import com.chatlycode.runtime.domain.CommandResult;

public interface RuntimeService {

    CommandResult run(CommandRequest request);
}
