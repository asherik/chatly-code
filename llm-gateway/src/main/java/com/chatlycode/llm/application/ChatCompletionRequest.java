package com.chatlycode.llm.application;

public record ChatCompletionRequest(
        String systemPrompt,
        String userPrompt,
        int maxTokens,
        double temperature
) {
    public ChatCompletionRequest {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = "You are a precise coding assistant.";
        }
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("User prompt must not be blank.");
        }
        if (maxTokens <= 0) {
            maxTokens = 2048;
        }
        if (temperature < 0 || temperature > 2) {
            temperature = 0.2;
        }
    }
}
