package com.workforceos.tenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.bootstrap.WorkforceOsApplication;
import com.workforceos.organization.TenantContext;
import com.workforceos.schedule.Roster;
import com.workforceos.schedule.RosterRequest;
import com.workforceos.schedule.ScheduleEngine;
import com.workforceos.schedule.ShiftTemplate;
import com.workforceos.schedule.ShiftTemplateRequest;
import com.workforceos.workforce.Department;
import com.workforceos.workforce.DepartmentRequest;
import com.workforceos.workforce.DepartmentService;

@SpringBootTest(classes = WorkforceOsApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TenantIsolationTests {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ScheduleEngine scheduleEngine;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void writesAreForcedToTheCurrentTenantNotTheClaimedOrganization() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        TenantContext.set(first);
        Department department = departmentService.create(new DepartmentRequest(second, "Alpha"));
        Roster roster = scheduleEngine.createRoster(new RosterRequest(second, "Week A"));
        ShiftTemplate shift = scheduleEngine.createShift(new ShiftTemplateRequest(
                second, "Day", LocalTime.of(8, 0), LocalTime.of(16, 0), List.of()));

        assertEquals(first, department.organizationId());
        assertEquals(first, roster.organizationId());
        assertEquals(first, shift.organizationId());
    }

    @Test
    void departmentsAreInvisibleAcrossTenants() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        TenantContext.set(first);
        Department department = departmentService.create(new DepartmentRequest(first, "Alpha"));

        TenantContext.set(second);
        assertThrows(ResponseStatusException.class, () -> departmentService.findById(department.id()));
        assertTrue(departmentService.findAll().isEmpty());
        assertThrows(ResponseStatusException.class, () -> departmentService.deactivate(department.id()));

        TenantContext.set(first);
        assertEquals(1, departmentService.findAll().size());
    }

    @Test
    void rostersAndShiftsAreInvisibleAcrossTenants() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        TenantContext.set(first);
        Roster roster = scheduleEngine.createRoster(new RosterRequest(first, "Week A"));
        ShiftTemplate shift = scheduleEngine.createShift(new ShiftTemplateRequest(
                first, "Day", LocalTime.of(8, 0), LocalTime.of(16, 0), List.of()));

        TenantContext.set(second);
        assertThrows(ResponseStatusException.class, () -> scheduleEngine.findRoster(roster.id()));
        assertThrows(ResponseStatusException.class, () -> scheduleEngine.findShift(shift.id()));
        assertTrue(scheduleEngine.findAllRosters().isEmpty());
        assertTrue(scheduleEngine.findAllShifts().isEmpty());
    }
}