package com.chatlycode.llm.application;

import com.chatlycode.llm.domain.ModelProfile;

public record LlmGatewayStatus(
        boolean configured,
        ModelProfile profile,
        String endpoint,
        String message
) {
}
