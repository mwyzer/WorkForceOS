package com.workforceos.scheduling;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.workforceos.schedule.Roster;
import com.workforceos.schedule.RosterAssignment;
import com.workforceos.schedule.ScheduleEngine;
import com.workforceos.schedule.ShiftTemplate;
import com.workforceos.workforce.Employee;
import com.workforceos.workforce.EmployeeService;
import com.workforceos.workforce.LeaveRequest;
import com.workforceos.workforce.LeaveRequestService;
import com.workforceos.workforce.LeaveRequestStatus;

/**
 * Proposes roster assignments to cover understaffed shift occurrences. It is deliberately advisory:
 * it returns a plan and never writes assignments, so a supervisor can review conflicts and leave
 * before applying. Eligibility excludes inactive, on-leave, already-assigned, and conflicting staff.
 */
@Service
public class AutoScheduleService {

    private final ScheduleEngine scheduleEngine;
    private final EmployeeService employeeService;
    private final LeaveRequestService leaveRequestService;
    private final int targetHeadcount;

    public AutoScheduleService(ScheduleEngine scheduleEngine, EmployeeService employeeService,
            LeaveRequestService leaveRequestService,
            @Value("${workforce.auto-schedule.target-headcount:1}") int targetHeadcount) {
        this.scheduleEngine = scheduleEngine;
        this.employeeService = employeeService;
        this.leaveRequestService = leaveRequestService;
        this.targetHeadcount = Math.max(1, targetHeadcount);
    }

    public AutoSchedulePlan propose(UUID rosterId) {
        Roster roster = scheduleEngine.findRoster(rosterId);

        List<RosterAssignment> rosterAssignments = scheduleEngine.findAssignments(rosterId).stream()
                .filter(RosterAssignment::active)
                .toList();
        List<RosterAssignment> allAssignments = scheduleEngine.findAllAssignments().stream()
                .filter(RosterAssignment::active)
                .toList();
        List<Employee> candidates = employeeService.findAll().stream()
                .filter(Employee::active)
                .sorted(Comparator.comparing(Employee::employeeNumber,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<LeaveRequest> approvedLeave = leaveRequestService.findAll().stream()
                .filter(request -> request.status() == LeaveRequestStatus.APPROVED)
                .toList();
        Map<UUID, String> shiftNames = new LinkedHashMap<>();
        for (ShiftTemplate shift : scheduleEngine.findAllShifts()) {
            shiftNames.put(shift.id(), shift.name());
        }

        Map<ShiftSlot, List<RosterAssignment>> slots = new LinkedHashMap<>();
        for (RosterAssignment assignment : rosterAssignments) {
            slots.computeIfAbsent(new ShiftSlot(assignment.shiftTemplateId(), assignment.start(), assignment.end()),
                    key -> new ArrayList<>()).add(assignment);
        }

        List<AutoScheduleProposal> proposals = new ArrayList<>();
        Set<String> proposedEmployees = new HashSet<>();
        int understaffedSlots = 0;

        for (Map.Entry<ShiftSlot, List<RosterAssignment>> entry : slots.entrySet()) {
            ShiftSlot slot = entry.getKey();
            List<RosterAssignment> current = entry.getValue();
            if (current.size() >= targetHeadcount) {
                continue;
            }
            understaffedSlots++;
            Set<UUID> assignedEmployees = new HashSet<>();
            current.forEach(assignment -> assignedEmployees.add(assignment.employeeId()));
            int needed = targetHeadcount - current.size();

            for (Employee candidate : candidates) {
                if (needed == 0) {
                    break;
                }
                if (assignedEmployees.contains(candidate.id())
                        || proposedEmployees.contains(slotKey(slot, candidate.id()))) {
                    continue;
                }
                if (onLeave(approvedLeave, candidate.id(), slot.start().toLocalDate())) {
                    continue;
                }
                boolean conflict = allAssignments.stream()
                        .filter(assignment -> assignment.employeeId().equals(candidate.id()))
                        .anyMatch(assignment -> slot.start().isBefore(assignment.end())
                                && slot.end().isAfter(assignment.start()));
                if (conflict) {
                    continue;
                }

                proposals.add(new AutoScheduleProposal(
                        slot.shiftTemplateId(),
                        shiftNames.get(slot.shiftTemplateId()),
                        candidate.id(),
                        candidate.employeeNumber(),
                        candidate.firstName(),
                        candidate.lastName(),
                        slot.start(),
                        slot.end(),
                        "Covers understaffed shift on " + slot.start().toLocalDate()));
                proposedEmployees.add(slotKey(slot, candidate.id()));
                needed--;
            }
        }

        return new AutoSchedulePlan(rosterId, roster.name(), targetHeadcount, understaffedSlots, proposals);
    }

    private boolean onLeave(List<LeaveRequest> leave, UUID employeeId, LocalDate date) {
        return leave.stream()
                .filter(request -> request.employeeId().equals(employeeId))
                .anyMatch(request -> !date.isBefore(request.startDate()) && !date.isAfter(request.endDate()));
    }

    private static String slotKey(ShiftSlot slot, UUID employeeId) {
        return slot.shiftTemplateId() + "|" + slot.start() + "|" + employeeId;
    }

    private record ShiftSlot(UUID shiftTemplateId, OffsetDateTime start, OffsetDateTime end) {
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ShiftSlot slot)) {
                return false;
            }
            return shiftTemplateId.equals(slot.shiftTemplateId)
                    && start.isEqual(slot.start)
                    && end.isEqual(slot.end);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(shiftTemplateId, start.toInstant(), end.toInstant());
        }
    }
}