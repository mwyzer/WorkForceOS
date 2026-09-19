package com.workforceos.risk;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workforceos.ai.WorkforceLlmClient;

@Component
public class LlmRiskAdvisor implements RiskAdvisorPort {

    record AiAdvice(String explanation, List<AiRecommendation> recommendations) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AiRecommendation(String riskType, String title, String description, String actionType,
            String actionEndpoint) {
    }

    private static final String PROVIDER_KEY = "${workforce.risk.ai-provider:none}";
    private static final String GENERIC_BASE_URL = "${workforce.risk.ai-base-url:}";
    private static final String GENERIC_MODEL = "${workforce.risk.ai-model:}";
    private static final String GENERIC_API_KEY = "${workforce.risk.ai-api-key:}";
    private static final String GENERIC_TIMEOUT = "${workforce.risk.ai-timeout-ms:15000}";

    private static final String MUSE_BASE_URL = "${workforce.risk.muse-base-url:}";
    private static final String MUSE_MODEL = "${workforce.risk.muse-model:}";
    private static final String MUSE_API_KEY = "${workforce.risk.muse-api-key:}";
    private static final String MUSE_TIMEOUT = "${workforce.risk.muse-timeout-ms:15000}";

    private static final String SPARK_BASE_URL = "${workforce.risk.spark-base-url:}";
    private static final String SPARK_MODEL = "${workforce.risk.spark-model:}";
    private static final String SPARK_API_KEY = "${workforce.risk.spark-api-key:}";
    private static final String SPARK_TIMEOUT = "${workforce.risk.spark-timeout-ms:15000}";

    private static final String SYSTEM_PROMPT = """
            You are a workforce risk analyst for a shift-based operations platform. \
            You receive a structured workforce risk snapshot and must explain the business impact and propose \
            actionable mitigation. Respond with valid JSON only, in this shape: \
            {"explanation": "<string>", "recommendations": [{"riskType": "<string>", "title": "<string>", \
            "description": "<string>", "actionType": "<string>", "actionEndpoint": "<string>"}]}. \
            Do not include personal data beyond what is provided.
            """;

    private final ObjectMapper mapper;
    private final WorkforceLlmClient client;

    public LlmRiskAdvisor(ObjectMapper mapper,
            @Value(PROVIDER_KEY) String provider,
            @Value(GENERIC_BASE_URL) String openaiBaseUrl,
            @Value(GENERIC_MODEL) String openaiModel,
            @Value(GENERIC_API_KEY) String openaiApiKey,
            @Value(GENERIC_TIMEOUT) int openaiTimeoutMs,
            @Value(MUSE_BASE_URL) String museBaseUrl,
            @Value(MUSE_MODEL) String museModel,
            @Value(MUSE_API_KEY) String museApiKey,
            @Value(MUSE_TIMEOUT) int museTimeoutMs,
            @Value(SPARK_BASE_URL) String sparkBaseUrl,
            @Value(SPARK_MODEL) String sparkModel,
            @Value(SPARK_API_KEY) String sparkApiKey,
            @Value(SPARK_TIMEOUT) int sparkTimeoutMs) {
        this.mapper = mapper;
        this.client = new WorkforceLlmClient(mapper, provider,
                new WorkforceLlmClient.ProviderSettings(openaiBaseUrl, openaiModel, openaiApiKey, openaiTimeoutMs),
                new WorkforceLlmClient.ProviderSettings(museBaseUrl, museModel, museApiKey, museTimeoutMs),
                new WorkforceLlmClient.ProviderSettings(sparkBaseUrl, sparkModel, sparkApiKey, sparkTimeoutMs));
    }

    public boolean available() {
        return client.available();
    }

    public String providerId() {
        return client.providerId();
    }

    @Override
    public RiskAdvice analyze(StructuredRisk structured) {
        String content = client.chat(0.3, SYSTEM_PROMPT,
                "Analyze this structured workforce risk snapshot:\n" + toJson(structured));

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

    private String toJson(StructuredRisk structured) {
        try {
            return mapper.writeValueAsString(structured);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build LLM request payload", ex);
        }
    }
}