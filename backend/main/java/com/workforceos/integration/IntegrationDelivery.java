package com.workforceos.integration;

public record IntegrationDelivery(
        String integration,
        DeliveryStatus status,
        String detail) {
}