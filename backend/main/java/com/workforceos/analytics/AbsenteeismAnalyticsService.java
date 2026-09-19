package com.workforceos.analytics;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.workforceos.attendance.AttendanceCalculation;
import com.workforceos.attendance.AttendanceEngine;
import com.workforceos.schedule.RosterAssignment;
import com.workforceos.schedule.ScheduleEngine;
import com.workforceos.workforce.Employee;
import com.workforceos.workforce.EmployeeService;
import com.workforceos.workforce.LeaveRequest;
import com.workforceos.workforce.LeaveRequestService;
import com.workforceos.workforce.LeaveRequestStatus;

/**
 * Explains historical attendance and approved leave to surface per-employee absenteeism risk. The
 * model is intentionally transparent (rates, trend, weighted score) so results are auditable and
 * work offline; it is not a black-box prediction.
 */
@Service
public class AbsenteeismAnalyticsService {

    private static final int MIN_WINDOW_DAYS = 1;
    private static final int MAX_WINDOW_DAYS = 365;

    private final EmployeeService employeeService;
    private final ScheduleEngine scheduleEngine;
    private final AttendanceEngine attendanceEngine;
    private final LeaveRequestService leaveRequestService;
    private final int defaultWindowDays;

    public AbsenteeismAnalyticsService(EmployeeService employeeService, ScheduleEngine scheduleEngine,
            AttendanceEngine attendanceEngine, LeaveRequestService leaveRequestService,
            @Value("${workforce.analytics.absenteeism-window-days:30}") int defaultWindowDays) {
        this.employeeService = employeeService;
        this.scheduleEngine = scheduleEngine;
        this.attendanceEngine = attendanceEngine;
        this.leaveRequestService = leaveRequestService;
        this.defaultWindowDays = defaultWindowDays;
    }

    @Cacheable(value = "absenteeism-report", key = "T(com.workforceos.organization.TenantContext).require().toString()")
    public AbsenteeismReport report(Integer windowDays) {
        int window = normalizeWindow(windowDays);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime windowStart = now.minusDays(window);
        OffsetDateTime midpoint = windowStart.plusDays(window / 2);

        Map<UUID, Employee> employeesById = employeeService.findAll().stream()
                .collect(Collectors.toMap(Employee::id, employee -> employee));

        List<AttendanceCalculation> calculations = attendanceEngine.calculateAll();
        Set<String> attendedShifts = calculations.stream()
                .map(calculation -> key(calculation.session().employeeId(), calculation.session().rosterId(),
                        calculation.session().shiftTemplateId()))
                .collect(Collectors.toCollection(HashSet::new));
        Map<UUID, Long> lateByEmployee = calculations.stream()
                .filter(calculation -> calculation.session().clockInAt() != null
                        && !calculation.session().clockInAt().isBefore(windowStart))
                .filter(AttendanceCalculation::late)
                .collect(Collectors.groupingBy(calculation -> calculation.session().employeeId(),
                        Collectors.counting()));

        List<RosterAssignment> completedAssignments = scheduleEngine.findAllAssignments().stream()
                .filter(RosterAssignment::active)
                .filter(assignment -> assignment.end().isAfter(windowStart) && !assignment.end().isAfter(now))
                .toList();
        Map<UUID, List<RosterAssignment>> assignmentsByEmployee = completedAssignments.stream()
                .collect(Collectors.groupingBy(RosterAssignment::employeeId));

        Map<UUID, Long> leaveDaysByEmployee = plannedLeaveDays(leaveRequestService.findAll(),
                windowStart.toLocalDate(), now.toLocalDate());

        Set<UUID> employeeIds = new HashSet<>(assignmentsByEmployee.keySet());
        employeeIds.addAll(leaveDaysByEmployee.keySet());

        List<AbsenteeismInsight> insights = new ArrayList<>();
        long totalScheduled = 0;
        long totalAbsent = 0;

        for (UUID employeeId : employeeIds) {
            List<RosterAssignment> employeeAssignments = assignmentsByEmployee.getOrDefault(employeeId, List.of());
            long scheduled = employeeAssignments.size();
            long absent = employeeAssignments.stream()
                    .filter(assignment -> !attendedShifts.contains(key(assignment.employeeId(),
                            assignment.rosterId(), assignment.shiftTemplateId())))
                    .count();
            long late = lateByEmployee.getOrDefault(employeeId, 0L);
            long leaveDays = leaveDaysByEmployee.getOrDefault(employeeId, 0L);

            totalScheduled += scheduled;
            totalAbsent += absent;

            double absenceRate = scheduled == 0 ? 0.0 : (double) absent / scheduled;
            double lateRate = scheduled == 0 ? 0.0 : (double) late / scheduled;
            String trend = trend(employeeAssignments, attendedShifts, midpoint);
            int score = score(absenceRate, lateRate, trend);
            String riskLevel = riskLevel(score);

            Employee employee = employeesById.get(employeeId);
            insights.add(new AbsenteeismInsight(
                    employeeId,
                    employee != null ? employee.employeeNumber() : null,
                    employee != null ? employee.firstName() : null,
                    employee != null ? employee.lastName() : null,
                    scheduled,
                    absent,
                    late,
                    leaveDays,
                    round(absenceRate),
                    trend,
                    riskLevel,
                    score,
                    drivers(absenceRate, lateRate, trend, leaveDays)));
        }

        insights.sort(Comparator.comparingInt(AbsenteeismInsight::score).reversed()
                .thenComparing(AbsenteeismInsight::employeeNumber,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        long high = countByLevel(insights, "HIGH");
        long medium = countByLevel(insights, "MEDIUM");
        long low = countByLevel(insights, "LOW");

        return new AbsenteeismReport(
                java.time.Instant.now(),
                window,
                high,
                medium,
                low,
                round(totalScheduled == 0 ? 0.0 : (double) totalAbsent / totalScheduled),
                insights);
    }

    private String trend(List<RosterAssignment> assignments, Set<String> attendedShifts, OffsetDateTime midpoint) {
        List<RosterAssignment> firstHalf = assignments.stream()
                .filter(assignment -> assignment.end().isBefore(midpoint))
                .toList();
        List<RosterAssignment> secondHalf = assignments.stream()
                .filter(assignment -> !assignment.end().isBefore(midpoint))
                .toList();
        if (firstHalf.isEmpty() || secondHalf.isEmpty()) {
            return "STABLE";
        }
        double firstRate = absenceRate(firstHalf, attendedShifts);
        double secondRate = absenceRate(secondHalf, attendedShifts);
        double delta = secondRate - firstRate;
        if (delta > 0.15) {
            return "INCREASING";
        }
        if (delta < -0.15) {
            return "DECREASING";
        }
        return "STABLE";
    }

    private double absenceRate(List<RosterAssignment> assignments, Set<String> attendedShifts) {
        if (assignments.isEmpty()) {
            return 0.0;
        }
        long absent = assignments.stream()
                .filter(assignment -> !attendedShifts.contains(key(assignment.employeeId(),
                        assignment.rosterId(), assignment.shiftTemplateId())))
                .count();
        return (double) absent / assignments.size();
    }

    private int score(double absenceRate, double lateRate, String trend) {
        int trendAdjustment = switch (trend) {
            case "INCREASING" -> 10;
            case "DECREASING" -> -5;
            default -> 0;
        };
        double raw = absenceRate * 140 + lateRate * 60 + trendAdjustment;
        return (int) Math.round(Math.max(0, Math.min(100, raw)));
    }

    private String riskLevel(int score) {
        if (score >= 50) {
            return "HIGH";
        }
        if (score >= 25) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private List<String> drivers(double absenceRate, double lateRate, String trend, long leaveDays) {
        List<String> drivers = new ArrayList<>();
        if (absenceRate >= 0.25) {
            drivers.add("Absence rate " + percent(absenceRate));
        } else if (absenceRate >= 0.10) {
            drivers.add("Elevated absence rate " + percent(absenceRate));
        }
        if (lateRate >= 0.20) {
            drivers.add("Frequent lateness " + percent(lateRate));
        }
        if ("INCREASING".equals(trend)) {
            drivers.add("Worsening absence trend");
        }
        if (leaveDays >= 3) {
            drivers.add("High planned leave (" + leaveDays + " days)");
        }
        return List.copyOf(drivers);
    }

    private Map<UUID, Long> plannedLeaveDays(List<LeaveRequest> requests, LocalDate windowStart, LocalDate windowEnd) {
        Map<UUID, Long> daysByEmployee = new HashMap<>();
        for (LeaveRequest request : requests) {
            if (request.status() != LeaveRequestStatus.APPROVED) {
                continue;
            }
            LocalDate start = request.startDate().isBefore(windowStart) ? windowStart : request.startDate();
            LocalDate end = request.endDate().isAfter(windowEnd) ? windowEnd : request.endDate();
            if (end.isBefore(start)) {
                continue;
            }
            long days = end.toEpochDay() - start.toEpochDay() + 1;
            daysByEmployee.merge(request.employeeId(), days, Long::sum);
        }
        return daysByEmployee;
    }

    private long countByLevel(List<AbsenteeismInsight> insights, String level) {
        return insights.stream().filter(insight -> insight.riskLevel().equals(level)).count();
    }

    private int normalizeWindow(Integer windowDays) {
        int requested = windowDays == null ? defaultWindowDays : windowDays;
        return Math.max(MIN_WINDOW_DAYS, Math.min(MAX_WINDOW_DAYS, requested));
    }

    private static String key(UUID employeeId, UUID rosterId, UUID shiftTemplateId) {
        return employeeId + "|" + rosterId + "|" + shiftTemplateId;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static String percent(double rate) {
        return Math.round(rate * 100) + "%";
    }
}