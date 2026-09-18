package com.workforceos.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reference outbound webhook integration for HR/payroll systems that accept JSON callbacks. Disabled
 * unless a URL is configured, so the application boots without provider credentials.
 */
@Component
public class WebhookIntegration implements OutboundIntegration {

    private final ObjectMapper mapper;
    private final boolean enabled;
    private final String url;
    private final String apiKey;
    private final int timeoutMs;

    public WebhookIntegration(ObjectMapper mapper,
            @Value("${workforce.integrations.webhook.enabled:false}") boolean enabled,
            @Value("${workforce.integrations.webhook.url:}") String url,
            @Value("${workforce.integrations.webhook.api-key:}") String apiKey,
            @Value("${workforce.integrations.webhook.timeout-ms:5000}") int timeoutMs) {
        this.mapper = mapper;
        this.enabled = enabled;
        this.url = url == null ? "" : url.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.timeoutMs = timeoutMs;
    }

    @Override
    public String name() {
        return "webhook";
    }

    @Override
    public IntegrationType type() {
        return IntegrationType.HR;
    }

    @Override
    public boolean enabled() {
        return enabled && !url.isBlank();
    }

    @Override
    public IntegrationDelivery deliver(IntegrationEvent event) {
        if (!enabled()) {
            return new IntegrationDelivery(name(), DeliveryStatus.SKIPPED, "Integration disabled or URL not configured");
        }
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(timeoutMs);
            factory.setReadTimeout(timeoutMs);
            RestClient.builder().baseUrl(url).requestFactory(factory).build().post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (!apiKey.isBlank()) {
                            headers.setBearerAuth(apiKey);
                        }
                    })
                    .body(mapper.writeValueAsString(event))
                    .retrieve()
                    .toBodilessEntity();
            return new IntegrationDelivery(name(), DeliveryStatus.DELIVERED, url);
        } catch (Exception ex) {
            return new IntegrationDelivery(name(), DeliveryStatus.FAILED, ex.getMessage());
        }
    }
}