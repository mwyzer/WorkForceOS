package com.workforceos.schedule;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roster_assignments")
class RosterAssignmentEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "roster_id", nullable = false)
    private UUID rosterId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "shift_template_id", nullable = false)
    private UUID shiftTemplateId;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime start;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime end;

    @Column(nullable = false)
    private boolean active;

    protected RosterAssignmentEntity() {
    }

    RosterAssignmentEntity(RosterAssignment assignment) {
        this.id = assignment.id();
        this.organizationId = assignment.organizationId();
        this.rosterId = assignment.rosterId();
        this.employeeId = assignment.employeeId();
        this.shiftTemplateId = assignment.shiftTemplateId();
        this.start = assignment.start();
        this.end = assignment.end();
        this.active = assignment.active();
    }

    RosterAssignment toRecord() {
        return new RosterAssignment(id, organizationId, rosterId, employeeId, shiftTemplateId, start, end, active);
    }
}

interface RosterAssignmentRepository extends JpaRepository<RosterAssignmentEntity, UUID> {

    List<RosterAssignmentEntity> findAllByRosterId(UUID rosterId);
}

@Component
@Profile("!test")
class JpaRosterAssignmentStore implements RosterAssignmentStore {

    private final RosterAssignmentRepository repository;

    JpaRosterAssignmentStore(RosterAssignmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RosterAssignment> findAll() {
        return repository.findAll().stream().map(RosterAssignmentEntity::toRecord).toList();
    }

    @Override
    public List<RosterAssignment> findByRosterId(UUID rosterId) {
        return repository.findAllByRosterId(rosterId).stream().map(RosterAssignmentEntity::toRecord).toList();
    }

    @Override
    public RosterAssignment save(RosterAssignment assignment) {
        repository.save(new RosterAssignmentEntity(assignment));
        return assignment;
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }
}