package com.chatlycode.llm.provider;

import com.chatlycode.llm.application.LlmGateway;

public final class NoopLlmGateway implements LlmGateway {

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return "LLM provider is not configured. Deterministic local analysis is available.";
    }
}
