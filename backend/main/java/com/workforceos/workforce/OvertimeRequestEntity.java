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
@Table(name = "overtime_requests")
class OvertimeRequestEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "request_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Double hours;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OvertimeRequestStatus status;

    protected OvertimeRequestEntity() {
    }

    OvertimeRequestEntity(UUID id, UUID organizationId, UUID employeeId, LocalDate date, Double hours,
            String reason, OvertimeRequestStatus status) {
        this.id = id;
        this.organizationId = organizationId;
        this.employeeId = employeeId;
        this.date = date;
        this.hours = hours;
        this.reason = reason;
        this.status = status;
    }

    OvertimeRequestStatus getStatus() {
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
        this.status = OvertimeRequestStatus.APPROVED;
    }

    void reject() {
        this.status = OvertimeRequestStatus.REJECTED;
    }

    OvertimeRequest toRecord() {
        return new OvertimeRequest(id, employeeId, date, hours, reason, status);
    }
}
