package com.workforceos.assistant;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workforceos.ai.WorkforceLlmClient;

/**
 * Optional LLM-backed assistant. Configuration defaults to the risk advisor's provider settings so a
 * single OpenAI-compatible endpoint can serve both features. Supported providers: {@code openai},
 * {@code muse}, and {@code spark} (model alias {@code spark-1.3}). When unavailable, callers fall back
 * to {@link HeuristicAssistant}.
 */
@Component
public class LlmAssistant {

    private static final String PROVIDER_KEY = "${workforce.assistant.ai-provider:${workforce.risk.ai-provider:none}}";
    private static final String GENERIC_BASE_URL = "${workforce.assistant.ai-base-url:${workforce.risk.ai-base-url:}}";
    private static final String GENERIC_MODEL = "${workforce.assistant.ai-model:${workforce.risk.ai-model:}}";
    private static final String GENERIC_API_KEY = "${workforce.assistant.ai-api-key:${workforce.risk.ai-api-key:}}";
    private static final String GENERIC_TIMEOUT = "${workforce.assistant.ai-timeout-ms:${workforce.risk.ai-timeout-ms:15000}}";

    private static final String MUSE_BASE_URL = "${workforce.assistant.muse-base-url:}";
    private static final String MUSE_MODEL = "${workforce.assistant.muse-model:}";
    private static final String MUSE_API_KEY = "${workforce.assistant.muse-api-key:}";
    private static final String MUSE_TIMEOUT = "${workforce.assistant.muse-timeout-ms:15000}";

    private static final String SPARK_BASE_URL = "${workforce.assistant.spark-base-url:}";
    private static final String SPARK_MODEL = "${workforce.assistant.spark-model:}";
    private static final String SPARK_API_KEY = "${workforce.assistant.spark-api-key:}";
    private static final String SPARK_TIMEOUT = "${workforce.assistant.spark-timeout-ms:15000}";

    private static final String SYSTEM_PROMPT = """
            You are the WorkforceOS workforce assistant for a shift-based operations platform. \
            Answer the user's question using only the JSON data snapshot provided. If the snapshot does \
            not contain the answer, say so plainly. Be concise, use business language, and never invent \
            numbers. Do not expose personal data beyond the aggregates provided. Respond with plain text.
            """;

    private final ObjectMapper mapper;
    private final WorkforceLlmClient client;

    public LlmAssistant(ObjectMapper mapper,
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

    public AssistantAnswer answer(String question, AssistantSnapshot snapshot, List<AssistantTopic> topics) {
        String content = client.chat(0.2, SYSTEM_PROMPT,
                "Question: " + question + "\n\nData snapshot:\n" + toJson(snapshot));
        return new AssistantAnswer(question, content.trim(), topics, "llm");
    }

    private String toJson(AssistantSnapshot snapshot) {
        try {
            return mapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build LLM request payload", ex);
        }
    }
}