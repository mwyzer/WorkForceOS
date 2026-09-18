package com.workforceos.assistant;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Optional LLM-backed assistant. Configuration defaults to the risk advisor's provider settings so a
 * single OpenAI-compatible endpoint can serve both features. When unavailable, callers fall back to
 * {@link HeuristicAssistant}.
 */
@Component
public class LlmAssistant {

    private static final String SYSTEM_PROMPT = """
            You are the WorkforceOS workforce assistant for a shift-based operations platform. \
            Answer the user's question using only the JSON data snapshot provided. If the snapshot does \
            not contain the answer, say so plainly. Be concise, use business language, and never invent \
            numbers. Do not expose personal data beyond the aggregates provided. Respond with plain text.
            """;

    private final ObjectMapper mapper;
    private final String provider;
    private final String baseUrl;
    private final String model;
    private final String apiKey;
    private final int timeoutMs;

    public LlmAssistant(ObjectMapper mapper,
            @Value("${workforce.assistant.ai-provider:${workforce.risk.ai-provider:none}}") String provider,
            @Value("${workforce.assistant.ai-base-url:${workforce.risk.ai-base-url:}}") String baseUrl,
            @Value("${workforce.assistant.ai-model:${workforce.risk.ai-model:}}") String model,
            @Value("${workforce.assistant.ai-api-key:${workforce.risk.ai-api-key:}}") String apiKey,
            @Value("${workforce.assistant.ai-timeout-ms:${workforce.risk.ai-timeout-ms:15000}}") int timeoutMs) {
        this.mapper = mapper;
        this.provider = provider;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.model = model == null ? "" : model.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.timeoutMs = timeoutMs;
    }

    public boolean available() {
        return "openai".equalsIgnoreCase(provider) && !baseUrl.isBlank() && !model.isBlank();
    }

    public AssistantAnswer answer(String question, AssistantSnapshot snapshot, List<AssistantTopic> topics) {
        if (!available()) {
            throw new IllegalStateException("LLM assistant is not configured");
        }
        String responseBody = restClient().post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (!apiKey.isBlank()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .body(buildPayload(question, snapshot))
                .retrieve()
                .body(String.class);

        String content = extractContent(responseBody);
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("LLM assistant returned an empty answer");
        }
        return new AssistantAnswer(question, content.trim(), topics, "llm");
    }

    private RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    private String buildPayload(String question, AssistantSnapshot snapshot) {
        try {
            String snapshotJson = mapper.writeValueAsString(snapshot);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("temperature", 0.2);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content",
                            "Question: " + question + "\n\nData snapshot:\n" + snapshotJson)));
            return mapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build LLM request payload", ex);
        }
    }

    private String extractContent(String responseBody) {
        try {
            return mapper.readTree(responseBody)
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse LLM response", ex);
        }
    }
}