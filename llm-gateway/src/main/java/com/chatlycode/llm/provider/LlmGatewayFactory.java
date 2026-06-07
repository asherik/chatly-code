package com.chatlycode.llm.provider;

import com.chatlycode.llm.application.LlmGateway;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;

public final class LlmGatewayFactory {

    private static final String DEFAULT_PROVIDER = "zai";

    private LlmGatewayFactory() {
    }

    public static LlmGateway createFromEnvironment() {
        String provider = value("chatly.llm.provider", "CHATLY_LLM_PROVIDER", DEFAULT_PROVIDER)
                .toLowerCase(Locale.ROOT)
                .trim();
        if (provider.equals("none") || provider.equals("noop") || provider.isBlank()) {
            return new NoopLlmGateway();
        }
        if (provider.equals("zai") || provider.equals("z.ai") || provider.equals("z-ai")) {
            return createZai();
        }
        return new NoopLlmGateway();
    }

    private static LlmGateway createZai() {
        String apiKey = firstValue(
                property("chatly.llm.zai.apiKey"),
                property("zai.api.key"),
                env("ZAI_API_KEY"),
                env("Z_AI_API_KEY"),
                env("CHATLY_ZAI_API_KEY")
        );
        if (apiKey.isBlank()) {
            return new NoopLlmGateway();
        }

        ZaiLlmGateway.Config config = new ZaiLlmGateway.Config(
                value("chatly.llm.zai.baseUrl", "ZAI_BASE_URL", ZaiLlmGateway.Config.DEFAULT_BASE_URL),
                value("chatly.llm.zai.model", "ZAI_MODEL", ZaiLlmGateway.Config.DEFAULT_MODEL),
                apiKey,
                intValue("chatly.llm.zai.maxTokens", "ZAI_MAX_TOKENS", 4096),
                doubleValue("chatly.llm.zai.temperature", "ZAI_TEMPERATURE", 0.2),
                Duration.ofSeconds(intValue("chatly.llm.zai.timeoutSeconds", "ZAI_TIMEOUT_SECONDS", 90))
        );
        return new ZaiLlmGateway(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build(), config);
    }

    private static String value(String propertyName, String envName, String defaultValue) {
        String value = firstValue(property(propertyName), env(envName));
        return value.isBlank() ? defaultValue : value;
    }

    private static int intValue(String propertyName, String envName, int defaultValue) {
        String value = value(propertyName, envName, Integer.toString(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static double doubleValue(String propertyName, String envName, double defaultValue) {
        String value = value(propertyName, envName, Double.toString(defaultValue));
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static String firstValue(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String property(String name) {
        return System.getProperty(name, "");
    }

    private static String env(String name) {
        return System.getenv().getOrDefault(name, "");
    }
}
