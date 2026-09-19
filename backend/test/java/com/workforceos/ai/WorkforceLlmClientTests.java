package com.workforceos.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class WorkforceLlmClientTests {

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<HttpServer> servers = new ArrayList<>();

    @AfterEach
    void stopServers() {
        servers.forEach(server -> server.stop(0));
        servers.clear();
    }

    private static WorkforceLlmClient.ProviderSettings settings(String baseUrl, String model) {
        return new WorkforceLlmClient.ProviderSettings(baseUrl, model, "secret-key", 5000);
    }

    private static WorkforceLlmClient.ProviderSettings unset() {
        return new WorkforceLlmClient.ProviderSettings("", "", "", 5000);
    }

    @Test
    void resolvesNoneWhenProviderIsBlank() {
        WorkforceLlmClient client = new WorkforceLlmClient(mapper, "", unset(), unset(), unset());
        assertThat(client.providerId()).isEqualTo("none");
        assertThat(client.available()).isFalse();
    }

    @Test
    void resolvesNoneForUnknownProvider() {
        WorkforceLlmClient client = new WorkforceLlmClient(mapper, "gpt-5", unset(), unset(), unset());
        assertThat(client.providerId()).isEqualTo("none");
        assertThat(client.available()).isFalse();
    }

    @Test
    void openaiIsAvailableWhenBaseUrlAndModelAreConfigured() {
        WorkforceLlmClient client = new WorkforceLlmClient(mapper,
                "openai", settings("https://api.openai.com/v1", "gpt-4o"), unset(), unset());
        assertThat(client.providerId()).isEqualTo("openai");
        assertThat(client.available()).isTrue();
    }

    @Test
    void openaiRequiresABaseUrl() {
        WorkforceLlmClient client = new WorkforceLlmClient(mapper,
                "openai", settings("", "gpt-4o"), unset(), unset());
        assertThat(client.available()).isFalse();
    }

    @Test
    void museIsAvailableWithoutModelUsingDefaultAlias() throws IOException {
        CapturingHandler handler = new CapturingHandler(
                "{\"choices\":[{\"message\":{\"content\":\"muse answer\"}}]}");
        String baseUrl = startServer(handler);
        WorkforceLlmClient client = new WorkforceLlmClient(mapper,
                "muse", unset(), settings(baseUrl, ""), unset());
        assertThat(client.available()).isTrue();
        assertThat(client.chat(0.2, "sys", "probe")).isEqualTo("muse answer");
        assertThat(handler.requestBody()).contains("\"model\":\"muse\"");
    }

    @Test
    void sparkUsesSpark13ModelAliasWhenModelIsBlank() throws IOException {
        CapturingHandler handler = new CapturingHandler(
                "{\"choices\":[{\"message\":{\"content\":\"spark answer\"}}]}");
        String baseUrl = startServer(handler);
        WorkforceLlmClient client = new WorkforceLlmClient(mapper,
                "spark", unset(), unset(), settings(baseUrl, ""));
        assertThat(client.providerId()).isEqualTo("spark");
        assertThat(client.available()).isTrue();
        assertThat(client.chat(0.3, "sys", "probe")).isEqualTo("spark answer");
        assertThat(handler.requestBody()).contains("\"model\":\"spark-1.3\"");
    }

    @Test
    void explicitModelOverridesDefaultAlias() throws IOException {
        CapturingHandler handler = new CapturingHandler(
                "{\"choices\":[{\"message\":{\"content\":\"spark answer\"}}]}");
        String baseUrl = startServer(handler);
        WorkforceLlmClient client = new WorkforceLlmClient(mapper,
                "spark", unset(), unset(), settings(baseUrl, "spark-2"));
        assertThat(client.chat(0.3, "sys", "probe")).isEqualTo("spark answer");
        assertThat(handler.requestBody()).contains("\"model\":\"spark-2\"");
    }

    @Test
    void sendsBearerApiKeyWhenConfigured() throws IOException {
        CapturingHandler handler = new CapturingHandler(
                "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");
        String baseUrl = startServer(handler);
        WorkforceLlmClient client = new WorkforceLlmClient(mapper,
                "openai", settings(baseUrl, "gpt-4o"), unset(), unset());
        client.chat(0.2, "sys", "probe");
        assertThat(handler.authorization()).isEqualTo("Bearer secret-key");
    }

    @Test
    void omitsAuthorizationHeaderWhenApiKeyIsBlank() throws IOException {
        CapturingHandler handler = new CapturingHandler(
                "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");
        String baseUrl = startServer(handler);
        WorkforceLlmClient client = new WorkforceLlmClient(mapper,
                "openai", new WorkforceLlmClient.ProviderSettings(baseUrl, "gpt-4o", "", 5000),
                unset(), unset());
        client.chat(0.2, "sys", "probe");
        assertThat(handler.authorization()).isNull();
        assertThat(handler.requestBody()).contains("gpt-4o");
    }

    @Test
    void chatFailsWhenProviderIsNotConfigured() {
        WorkforceLlmClient client = new WorkforceLlmClient(mapper, "none", unset(), unset(), unset());
        assertThrows(IllegalStateException.class, () -> client.chat(0.2, "sys", "probe"));
    }

    @Test
    void failsWhenProviderReturnsNoUsableContent() throws IOException {
        CapturingHandler handler = new CapturingHandler(
                "{\"choices\":[{\"message\":{\"content\":\"\"}}]}");
        String baseUrl = startServer(handler);
        WorkforceLlmClient client = new WorkforceLlmClient(mapper,
                "spark", unset(), unset(), settings(baseUrl, ""));
        assertThrows(IllegalStateException.class, () -> client.chat(0.3, "sys", "probe"));
    }

    private String startServer(CapturingHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", handler);
        server.start();
        servers.add(server);
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static final class CapturingHandler implements HttpHandler {

        private final String responseJson;
        private volatile String authorization;
        private volatile String requestBody;

        private CapturingHandler(String responseJson) {
            this.responseJson = responseJson;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            authorization = exchange.getRequestHeaders().getFirst("Authorization");
            requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }

        private String authorization() {
            return authorization;
        }

        private String requestBody() {
            return requestBody;
        }
    }
}