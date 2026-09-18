package com.workforceos.attendance;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance_sessions")
class AttendanceSessionEntity {

    @Id
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "roster_id", nullable = false)
    private UUID rosterId;

    @Column(name = "shift_template_id", nullable = false)
    private UUID shiftTemplateId;

    @Column(name = "clock_in_at", nullable = false)
    private OffsetDateTime clockInAt;

    @Column(name = "clock_out_at")
    private OffsetDateTime clockOutAt;

    @Column(nullable = false)
    private boolean active;

    protected AttendanceSessionEntity() {
    }

    AttendanceSessionEntity(AttendanceSession session) {
        this.id = session.id();
        this.employeeId = session.employeeId();
        this.rosterId = session.rosterId();
        this.shiftTemplateId = session.shiftTemplateId();
        this.clockInAt = session.clockInAt();
        this.clockOutAt = session.clockOutAt();
        this.active = session.active();
    }

    AttendanceSession toRecord() {
        return new AttendanceSession(id, employeeId, rosterId, shiftTemplateId, clockInAt, clockOutAt, active);
    }
}

interface AttendanceSessionRepository extends JpaRepository<AttendanceSessionEntity, UUID> {

    Optional<AttendanceSessionEntity> findByEmployeeIdAndActiveTrue(UUID employeeId);

    List<AttendanceSessionEntity> findAllByEmployeeIdOrderByClockInAtAsc(UUID employeeId);

    List<AttendanceSessionEntity> findAllByOrderByClockInAtAsc();
}

@Component
@Profile("!test")
class JpaAttendanceSessionStore implements AttendanceSessionStore {

    private final AttendanceSessionRepository repository;

    JpaAttendanceSessionStore(AttendanceSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AttendanceSession> findActiveByEmployeeId(UUID employeeId) {
        return repository.findByEmployeeIdAndActiveTrue(employeeId).map(AttendanceSessionEntity::toRecord);
    }

    @Override
    public List<AttendanceSession> findByEmployeeId(UUID employeeId) {
        return repository.findAllByEmployeeIdOrderByClockInAtAsc(employeeId).stream()
                .map(AttendanceSessionEntity::toRecord)
                .toList();
    }

    @Override
    public List<AttendanceSession> findAll() {
        return repository.findAllByOrderByClockInAtAsc().stream()
                .map(AttendanceSessionEntity::toRecord)
                .toList();
    }

    @Override
    public AttendanceSession save(AttendanceSession session) {
        repository.save(new AttendanceSessionEntity(session));
        return session;
    }
}