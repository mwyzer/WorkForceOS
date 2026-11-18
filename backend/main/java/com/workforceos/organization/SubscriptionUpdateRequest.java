package com.workforceos.organization;

public record SubscriptionUpdateRequest(
        String planCode,
        String billingStatus) {
}
