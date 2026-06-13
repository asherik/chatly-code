package com.chatlycode.llm.provider;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ZaiLlmGatewayTest {

    @Test
    void returnsAssistantContentFromChatCompletionResponse() {
        ZaiLlmGateway gateway = new ZaiLlmGateway(
                new StubHttpClient(200, """
                        {"choices":[{"message":{"role":"assistant","content":"Hello\\nworld \\u2713"}}]}
                        """),
                config()
        );

        String response = gateway.complete("system", "user");

        assertEquals("Hello\nworld \u2713", response);
    }

    @Test
    void throwsOnHttpError() {
        ZaiLlmGateway gateway = new ZaiLlmGateway(
                new StubHttpClient(401, "{\"error\":{\"message\":\"bad key\"}}"),
                config()
        );

        assertThrows(LlmGatewayException.class, () -> gateway.complete("system", "user"));
    }

    private ZaiLlmGateway.Config config() {
        return new ZaiLlmGateway.Config(
                "https://api.z.ai/api/paas/v4",
                "glm-test",
                "test-key",
                512,
                0.1,
                Duration.ofSeconds(5)
        );
    }

    private static final class StubHttpClient extends HttpClient {

        private final int statusCode;
        private final String body;

        private StubHttpClient(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
            @SuppressWarnings("unchecked")
            T typedBody = (T) body;
            return new StubHttpResponse<>(request, statusCode, typedBody);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            throw new UnsupportedOperationException("Async calls are not used by this test.");
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            throw new UnsupportedOperationException("Async calls are not used by this test.");
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return null;
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_2;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }
    }

    private record StubHttpResponse<T>(
            HttpRequest request,
            int statusCode,
            T body
    ) implements HttpResponse<T> {

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(java.util.Map.of(), (name, value) -> true);
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public Version version() {
            return Version.HTTP_2;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }
    }
}
