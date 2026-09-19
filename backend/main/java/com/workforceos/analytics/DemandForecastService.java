package com.workforceos.analytics;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.workforceos.schedule.RosterAssignment;
import com.workforceos.schedule.ScheduleEngine;
import com.workforceos.workforce.Employee;
import com.workforceos.workforce.EmployeeService;
import com.workforceos.workforce.LeaveRequest;
import com.workforceos.workforce.LeaveRequestService;
import com.workforceos.workforce.LeaveRequestStatus;

/**
 * Forecasts near-term staffing demand by averaging historical assignment volume for each weekday
 * and comparing it with what is already scheduled and who is on approved leave. Transparent and
 * offline so results are explainable; a statistical model can replace the averaging later.
 */
@Service
public class DemandForecastService {

    private static final int MAX_HORIZON_DAYS = 30;
    private static final int MAX_LOOKBACK_WEEKS = 26;

    private final EmployeeService employeeService;
    private final ScheduleEngine scheduleEngine;
    private final LeaveRequestService leaveRequestService;
    private final int defaultHorizonDays;
    private final int lookbackWeeks;

    public DemandForecastService(EmployeeService employeeService, ScheduleEngine scheduleEngine,
            LeaveRequestService leaveRequestService,
            @Value("${workforce.analytics.forecast-horizon-days:7}") int defaultHorizonDays,
            @Value("${workforce.analytics.forecast-lookback-weeks:4}") int lookbackWeeks) {
        this.employeeService = employeeService;
        this.scheduleEngine = scheduleEngine;
        this.leaveRequestService = leaveRequestService;
        this.defaultHorizonDays = defaultHorizonDays;
        this.lookbackWeeks = Math.max(1, Math.min(MAX_LOOKBACK_WEEKS, lookbackWeeks));
    }

    @Cacheable(value = "demand-forecast", key = "T(com.workforceos.organization.TenantContext).require().toString()")
    public DemandForecast forecast(Integer horizonDays) {
        int horizon = clampHorizon(horizonDays);
        LocalDate today = LocalDate.now();

        long activeEmployees = employeeService.findAll().stream().filter(Employee::active).count();

        List<RosterAssignment> assignments = scheduleEngine.findAllAssignments().stream()
                .filter(RosterAssignment::active)
                .toList();
        List<LeaveRequest> approvedLeave = leaveRequestService.findAll().stream()
                .filter(request -> request.status() == LeaveRequestStatus.APPROVED)
                .toList();

        Map<DayOfWeek, List<Long>> history = historicalVolume(assignments, today, lookbackWeeks);

        List<DailyDemand> days = new ArrayList<>();
        long totalExpected = 0;
        long totalScheduled = 0;
        long shortageDays = 0;

        for (int offset = 0; offset < horizon; offset++) {
            LocalDate date = today.plusDays(offset);
            long expectedDemand = average(history.get(date.getDayOfWeek()));
            long scheduled = assignments.stream().filter(assignment -> coversDate(assignment, date)).count();
            long onLeave = approvedLeave.stream()
                    .filter(request -> coversDate(request, date))
                    .map(LeaveRequest::employeeId)
                    .distinct()
                    .count();
            long available = Math.max(0, activeEmployees - onLeave);
            long gap = expectedDemand - scheduled;
            boolean shortage = gap > 0;

            totalExpected += expectedDemand;
            totalScheduled += scheduled;
            if (shortage) {
                shortageDays++;
            }
            days.add(new DailyDemand(date, date.getDayOfWeek().name(), expectedDemand, scheduled, onLeave,
                    available, gap, shortage));
        }

        return new DemandForecast(java.time.Instant.now(), horizon, lookbackWeeks, totalExpected,
                totalScheduled, shortageDays, days);
    }

    private Map<DayOfWeek, List<Long>> historicalVolume(List<RosterAssignment> assignments, LocalDate today,
            int weeks) {
        Map<LocalDate, Long> perDay = assignments.stream()
                .collect(Collectors.groupingBy(assignment -> assignment.start().toLocalDate(),
                        Collectors.counting()));

        Map<DayOfWeek, List<Long>> history = new EnumMap<>(DayOfWeek.class);
        LocalDate start = today.minusWeeks(weeks);
        for (LocalDate date = start; date.isBefore(today); date = date.plusDays(1)) {
            history.computeIfAbsent(date.getDayOfWeek(), key -> new ArrayList<>())
                    .add(perDay.getOrDefault(date, 0L));
        }
        return history;
    }

    private long average(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        return Math.round(values.stream().mapToLong(Long::longValue).average().orElse(0.0));
    }

    private boolean coversDate(RosterAssignment assignment, LocalDate date) {
        LocalDate start = assignment.start().toLocalDate();
        LocalDate end = assignment.end().toLocalDate();
        return !start.isAfter(date) && !end.isBefore(date);
    }

    private boolean coversDate(LeaveRequest request, LocalDate date) {
        return !date.isBefore(request.startDate()) && !date.isAfter(request.endDate());
    }

    private int clampHorizon(Integer requested) {
        int value = requested == null ? defaultHorizonDays : requested;
        return Math.max(1, Math.min(MAX_HORIZON_DAYS, value));
    }
}