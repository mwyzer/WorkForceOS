package com.workforceos.organization;

import java.util.List;
import java.util.UUID;

public record TenantSubscription(
        UUID organizationId,
        String planCode,
        String planName,
        String billingStatus,
        int seatLimit,
        long seatsUsed,
        boolean compliant,
        List<String> enabledFeatures) {
}
