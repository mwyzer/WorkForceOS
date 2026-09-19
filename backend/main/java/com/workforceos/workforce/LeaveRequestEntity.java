package com.workforceos.workforce;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "leave_requests")
class LeaveRequestEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveRequestStatus status;

    protected LeaveRequestEntity() {
    }

    LeaveRequestEntity(UUID id, UUID organizationId, UUID employeeId, LocalDate startDate, LocalDate endDate,
            String reason, LeaveRequestStatus status) {
        this.id = id;
        this.organizationId = organizationId;
        this.employeeId = employeeId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = status;
    }

    LeaveRequestStatus getStatus() {
        return status;
    }

    UUID getId() {
        return id;
    }

    UUID getEmployeeId() {
        return employeeId;
    }

    UUID organizationId() {
        return organizationId;
    }

    void approve() {
        this.status = LeaveRequestStatus.APPROVED;
    }

    LeaveRequest toRecord() {
        return new LeaveRequest(id, employeeId, startDate, endDate, reason, status);
    }
}
