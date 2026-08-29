package com.workforceos.scheduling;

public record LeaveRequestReport(
        long pending,
        long approved,
        long rejected) {
}
