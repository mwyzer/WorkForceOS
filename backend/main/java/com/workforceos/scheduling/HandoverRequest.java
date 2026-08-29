package com.workforceos.scheduling;

import java.util.List;
import java.util.UUID;

public record HandoverRequest(
        UUID employeeId,
        List<HandoverItem> items) {
}
