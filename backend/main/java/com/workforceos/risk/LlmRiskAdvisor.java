package com.workforceos.risk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class LlmRiskAdvisor implements RiskAdvisorPort {

    record AiAdvice(String explanation, List<AiRecommendation> recommendations) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AiRecommendation(String riskType, String title, String description, String actionType,
            String actionEndpoint) {
    }

    private static final String SYSTEM_PROMPT = """
            You are a workforce risk analyst for a shift-based operations platform. \
            You receive a structured workforce risk snapshot and must explain the business impact and propose \
            actionable mitigation. Respond with valid JSON only, in this shape: \
            {"explanation": "<string>", "recommendations": [{"riskType": "<string>", "title": "<string>", \
            "description": "<string>", "actionType": "<string>", "actionEndpoint": "<string>"}]}. \
            Do not include personal data beyond what is provided.
            """;

    private final ObjectMapper mapper;
    private final String provider;
    private final String baseUrl;
    private final String model;
    private final String apiKey;
    private final int timeoutMs;

    public LlmRiskAdvisor(ObjectMapper mapper,
            @Value("${workforce.risk.ai-provider:none}") String provider,
            @Value("${workforce.risk.ai-base-url:}") String baseUrl,
            @Value("${workforce.risk.ai-model:}") String model,
            @Value("${workforce.risk.ai-api-key:}") String apiKey,
            @Value("${workforce.risk.ai-timeout-ms:15000}") int timeoutMs) {
        this.mapper = mapper;
        this.provider = provider;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.model = model == null ? "" : model.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.timeoutMs = timeoutMs;
    }

    public boolean available() {
        return "openai".equalsIgnoreCase(provider)
                && !baseUrl.isBlank()
                && !model.isBlank();
    }

    @Override
    public RiskAdvice analyze(StructuredRisk structured) {
        if (!available()) {
            throw new IllegalStateException("LLM advisor is not configured");
        }
        String requestBody = buildPayload(structured);
        String responseBody = restClient().post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (!apiKey.isBlank()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .body(requestBody)
                .retrieve()
                .body(String.class);

        String content = extractContent(responseBody);
        AiAdvice advice;
        try {
            advice = mapper.readValue(content, AiAdvice.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse LLM response", ex);
        }
        List<RiskRecommendation> recommendations = advice.recommendations() == null ? List.of()
                : advice.recommendations().stream()
                        .map(rec -> new RiskRecommendation(rec.riskType(), rec.title(), rec.description(),
                                rec.actionType(), rec.actionEndpoint()))
                        .toList();
        return new RiskAdvice(
                advice.explanation() == null ? "No explanation provided." : advice.explanation(),
                structured.impact(),
                recommendations,
                mode());
    }

    @Override
    public String mode() {
        return "llm";
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

    private String buildPayload(StructuredRisk structured) {
        try {
            String riskJson = mapper.writeValueAsString(structured);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("temperature", 0.3);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", "Analyze this structured workforce risk snapshot:\n" + riskJson)));
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