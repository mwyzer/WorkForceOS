package com.workforceos.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Shared OpenAI-compatible chat client used by the workforce assistant and the risk advisor.
 *
 * <p>A module selects a provider by name ({@code openai}, {@code muse}, or {@code spark}) and the
 * client resolves that provider's base URL, model, API key, and timeout from the settings supplied
 * at construction. Providers are config-gated: a provider is unused until it is explicitly selected
 * AND a base URL is configured, and every provider accepts an API key header when provided. When the
 * model is left blank for the {@code muse} or {@code spark} provider, a default alias is applied
 * ({@code muse} and {@code spark-1.3} respectively).
 */
public final class WorkforceLlmClient {

    public static final String PROVIDER_NONE = "none";
    public static final String PROVIDER_OPENAI = "openai";
    public static final String PROVIDER_MUSE = "muse";
    public static final String PROVIDER_SPARK = "spark";

    private static final String DEFAULT_MUSE_MODEL = "muse";
    private static final String DEFAULT_SPARK_MODEL = "spark-1.3";

    public record ProviderSettings(String baseUrl, String model, String apiKey, int timeoutMs) {

        public ProviderSettings {
            baseUrl = baseUrl == null ? "" : baseUrl.trim();
            model = model == null ? "" : model.trim();
            apiKey = apiKey == null ? "" : apiKey.trim();
            timeoutMs = timeoutMs <= 0 ? 15000 : timeoutMs;
        }
    }

    private final ObjectMapper mapper;
    private final String providerId;
    private final ProviderSettings openai;
    private final ProviderSettings muse;
    private final ProviderSettings spark;

    public WorkforceLlmClient(ObjectMapper mapper, String configuredProvider,
            ProviderSettings openai, ProviderSettings muse, ProviderSettings spark) {
        this.mapper = mapper;
        this.providerId = resolve(configuredProvider);
        this.openai = openai == null ? new ProviderSettings("", "", "", 15000) : openai;
        this.muse = muse == null ? new ProviderSettings("", "", "", 15000) : muse;
        this.spark = spark == null ? new ProviderSettings("", "", "", 15000) : spark;
    }

    /** Canonical provider selected at construction; {@code none} when unconfigured or unknown. */
    public String providerId() {
        return providerId;
    }

    public boolean available() {
        ProviderSettings settings = active();
        return settings != null && !settings.baseUrl().isBlank() && !model().isBlank();
    }

    public String chat(double temperature, String systemPrompt, String userText) {
        if (!available()) {
            throw new IllegalStateException("LLM provider is not configured");
        }
        ProviderSettings settings = active();
        String responseBody = restClient(settings).post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (!settings.apiKey().isBlank()) {
                        headers.setBearerAuth(settings.apiKey());
                    }
                })
                .body(buildPayload(temperature, systemPrompt, userText))
                .retrieve()
                .body(String.class);

        String content = extractContent(responseBody);
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("LLM provider returned an empty answer");
        }
        return content;
    }

    private String resolve(String configuredProvider) {
        String normalized = configuredProvider == null ? "" : configuredProvider.trim().toLowerCase();
        return switch (normalized) {
            case PROVIDER_MUSE -> PROVIDER_MUSE;
            case PROVIDER_SPARK -> PROVIDER_SPARK;
            case PROVIDER_OPENAI -> PROVIDER_OPENAI;
            default -> PROVIDER_NONE;
        };
    }

    private ProviderSettings active() {
        return switch (providerId) {
            case PROVIDER_MUSE -> muse;
            case PROVIDER_SPARK -> spark;
            case PROVIDER_OPENAI -> openai;
            default -> null;
        };
    }

    private String model() {
        ProviderSettings settings = active();
        if (settings == null) {
            return "";
        }
        if (!settings.model().isBlank()) {
            return settings.model();
        }
        return switch (providerId) {
            case PROVIDER_MUSE -> DEFAULT_MUSE_MODEL;
            case PROVIDER_SPARK -> DEFAULT_SPARK_MODEL;
            default -> "";
        };
    }

    private RestClient restClient(ProviderSettings settings) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(settings.timeoutMs());
        factory.setReadTimeout(settings.timeoutMs());
        return RestClient.builder()
                .baseUrl(settings.baseUrl())
                .requestFactory(factory)
                .build();
    }

    private String buildPayload(double temperature, String systemPrompt, String userText) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model());
            payload.put("temperature", temperature);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userText)));
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