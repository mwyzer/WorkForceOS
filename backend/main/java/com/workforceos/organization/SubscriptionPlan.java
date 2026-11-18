package com.workforceos.organization;

import java.util.List;

public record SubscriptionPlan(
        String code,
        String name,
        int maxSeats,
        int monthlyPriceUsd,
        List<String> features) {
}
