package com.chatlycode.llm.provider;

import com.chatlycode.llm.application.ChatCompletionRequest;
import com.chatlycode.llm.application.LlmGateway;
import com.chatlycode.llm.application.LlmGatewayStatus;
import com.chatlycode.llm.domain.ModelProfile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class ZaiLlmGateway implements LlmGateway {

    private final HttpClient httpClient;
    private final Config config;
    private final URI completionsUri;

    public ZaiLlmGateway(HttpClient httpClient, Config config) {
        this.httpClient = httpClient;
        this.config = config;
        this.completionsUri = URI.create(normalizeBaseUrl(config.baseUrl()) + "/chat/completions");
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return complete(new ChatCompletionRequest(
                systemPrompt,
                userPrompt,
                config.maxTokens(),
                config.temperature()
        ));
    }

    public String complete(ChatCompletionRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder(completionsUri)
                .timeout(config.timeout())
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(request)))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new LlmGatewayException("Z.AI request failed with HTTP " + response.statusCode() + ": " + abbreviate(response.body()));
            }
            String content = extractAssistantContent(response.body());
            if (content.isBlank()) {
                throw new LlmGatewayException("Z.AI response did not contain assistant content.");
            }
            return content;
        } catch (IOException exception) {
            throw new LlmGatewayException("Z.AI request failed due to an I/O error.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmGatewayException("Z.AI request was interrupted.", exception);
        }
    }

    @Override
    public LlmGatewayStatus status() {
        return new LlmGatewayStatus(
                true,
                new ModelProfile("zai", config.model(), 131_072),
                completionsUri.toString(),
                "Z.AI provider is configured."
        );
    }

    private String toJson(ChatCompletionRequest request) {
        return """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "%s"},
                    {"role": "user", "content": "%s"}
                  ],
                  "temperature": %s,
                  "max_tokens": %d
                }
                """.formatted(
                escape(config.model()),
                escape(request.systemPrompt()),
                escape(request.userPrompt()),
                Double.toString(request.temperature()),
                request.maxTokens()
        );
    }

    private String extractAssistantContent(String json) {
        int contentKey = json.indexOf("\"content\"");
        if (contentKey < 0) {
            return "";
        }
        int colon = json.indexOf(':', contentKey);
        int firstQuote = json.indexOf('"', colon + 1);
        if (colon < 0 || firstQuote < 0) {
            return "";
        }
        StringBuilder content = new StringBuilder();
        boolean escaped = false;
        for (int index = firstQuote + 1; index < json.length(); index++) {
            char current = json.charAt(index);
            if (escaped) {
                if (current == 'u' && index + 4 < json.length()) {
                    content.append(unescapeUnicode(json.substring(index + 1, index + 5)));
                    index += 4;
                } else {
                    content.append(unescape(current));
                }
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                return content.toString();
            }
            content.append(current);
        }
        return "";
    }

    private char unescapeUnicode(String hex) {
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException exception) {
            return '?';
        }
    }

    private char unescape(char current) {
        return switch (current) {
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'b' -> '\b';
            case 'f' -> '\f';
            default -> current;
        };
    }

    private String escape(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 32);
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (current < 0x20) {
                        builder.append("\\u%04x".formatted((int) current));
                    } else {
                        builder.append(current);
                    }
                }
            }
        }
        return builder.toString();
    }

    private String abbreviate(String value) {
        if (value == null || value.length() <= 600) {
            return value == null ? "" : value;
        }
        return value.substring(0, 600) + "...";
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null || baseUrl.isBlank()
                ? Config.DEFAULT_BASE_URL
                : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.endsWith("/chat/completions")
                ? normalized.substring(0, normalized.length() - "/chat/completions".length())
                : normalized;
    }

    public record Config(
            String baseUrl,
            String model,
            String apiKey,
            int maxTokens,
            double temperature,
            Duration timeout
    ) {
        public static final String DEFAULT_BASE_URL = "https://api.z.ai/api/paas/v4";
        public static final String DEFAULT_MODEL = "glm-4.5";

        public Config {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalArgumentException("Z.AI API key must not be blank.");
            }
            if (model == null || model.isBlank()) {
                model = DEFAULT_MODEL;
            }
            if (maxTokens <= 0) {
                maxTokens = 4096;
            }
            if (temperature < 0 || temperature > 2) {
                temperature = 0.2;
            }
            if (timeout == null || timeout.isNegative() || timeout.isZero()) {
                timeout = Duration.ofSeconds(90);
            }
        }
    }
}
