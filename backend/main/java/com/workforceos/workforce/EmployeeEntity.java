package com.workforceos.workforce;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "employees")
class EmployeeEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "employee_number", nullable = false, unique = true)
    private String employeeNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(nullable = false)
    private boolean active;

    protected EmployeeEntity() {
    }

    EmployeeEntity(UUID id, UUID organizationId, String employeeNumber, String firstName, String lastName,
            String email, UUID departmentId, UUID teamId, boolean active) {
        this.id = id;
        this.organizationId = organizationId;
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.departmentId = departmentId;
        this.teamId = teamId;
        this.active = active;
    }

    void deactivate() {
        this.active = false;
    }

    void update(String firstName, String lastName, String email, UUID departmentId, UUID teamId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.departmentId = departmentId;
        this.teamId = teamId;
    }

    UUID organizationId() {
        return organizationId;
    }

    Employee toRecord() {
        return new Employee(id, employeeNumber, firstName, lastName, email, departmentId, teamId, active);
    }
}
