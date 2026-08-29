package com.workforceos.scheduling;

public record OvertimeRequestReport(
        long total,
        long approved,
        long rejected,
        long pending,
        Double totalHours) {
}
