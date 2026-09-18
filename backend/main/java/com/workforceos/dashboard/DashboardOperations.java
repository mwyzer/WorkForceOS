package com.workforceos.dashboard;

import java.util.List;

public record DashboardOperations(
        long pendingLeaveRequests,
        long pendingOvertimeRequests,
        long pendingHandovers,
        long publishedRosters,
        List<RecentNotification> recentNotifications) {
}