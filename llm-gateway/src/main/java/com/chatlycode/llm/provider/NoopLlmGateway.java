package com.chatlycode.llm.provider;

import com.chatlycode.llm.application.LlmGateway;
import com.chatlycode.llm.application.LlmGatewayStatus;
import com.chatlycode.llm.domain.ModelProfile;

public final class NoopLlmGateway implements LlmGateway {

    private static final ModelProfile PROFILE = new ModelProfile("none", "deterministic-local", 0);

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return "LLM provider is not configured. Deterministic local analysis is available.";
    }

    @Override
    public LlmGatewayStatus status() {
        return new LlmGatewayStatus(
                false,
                PROFILE,
                "",
                "LLM provider is not configured. Set CHATLY_LLM_PROVIDER=zai and ZAI_API_KEY to enable Z.AI."
        );
    }
}
