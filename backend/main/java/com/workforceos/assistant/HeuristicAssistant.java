package com.workforceos.assistant;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Offline answer generator. Every statement is derived from the read-only {@link AssistantSnapshot},
 * so the assistant returns grounded operational facts even when no LLM is configured.
 */
@Component
public class HeuristicAssistant {

    public AssistantAnswer answer(String question, AssistantSnapshot snapshot, List<AssistantTopic> topics) {
        String text = topics.stream()
                .map(topic -> describe(topic, snapshot))
                .collect(Collectors.joining(" "));
        return new AssistantAnswer(question, text, topics, "heuristic");
    }

    private String describe(AssistantTopic topic, AssistantSnapshot snapshot) {
        return switch (topic) {
            case HEADCOUNT -> "There are " + snapshot.employeeCount() + " employees ("
                    + snapshot.activeEmployees() + " active) across " + snapshot.departmentCount()
                    + " departments and " + snapshot.teamCount() + " teams.";
            case APPROVALS -> "Pending approvals: " + snapshot.pendingLeaveRequests() + " leave, "
                    + snapshot.pendingOvertimeRequests() + " overtime; "
                    + snapshot.pendingHandovers() + " handovers awaiting acknowledgement.";
            case ATTENDANCE -> "Attendance: " + snapshot.attendancePresent() + " present, "
                    + snapshot.attendanceLate() + " late, " + snapshot.attendanceAbsent() + " absent, "
                    + snapshot.attendanceOvertime() + " with overtime recorded.";
            case OVERTIME -> "Overtime: " + snapshot.overtimeTotalHours() + " total hours requested ("
                    + snapshot.overtimeApproved() + " approved, " + snapshot.overtimePending() + " pending).";
            case LEAVE -> "Leave requests: " + snapshot.leavePending() + " pending, "
                    + snapshot.leaveApproved() + " approved, " + snapshot.leaveRejected() + " rejected.";
            case RISK -> "Workforce risk index is " + snapshot.riskIndex() + " with "
                    + snapshot.openAlerts() + " open alerts and " + snapshot.highRiskAssessments()
                    + " high-severity assessments.";
            case ROSTER -> "There are " + snapshot.publishedRosters() + " published rosters and "
                    + snapshot.scheduleConflicts() + " detected schedule conflicts.";
            case GENERAL -> "Workforce overview: " + snapshot.employeeCount() + " employees ("
                    + snapshot.activeEmployees() + " active), " + snapshot.pendingLeaveRequests()
                    + " pending leave, " + snapshot.pendingOvertimeRequests() + " pending overtime, risk index "
                    + snapshot.riskIndex() + " with " + snapshot.openAlerts() + " open alerts.";
        };
    }
}