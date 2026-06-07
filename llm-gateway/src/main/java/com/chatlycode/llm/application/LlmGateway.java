package com.chatlycode.llm.application;

public interface LlmGateway {

    String complete(String systemPrompt, String userPrompt);
}
