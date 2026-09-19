package com.workforceos.attendance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance_corrections")
class AttendanceCorrectionEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "attendance_id", nullable = false)
    private UUID attendanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "correction_type", nullable = false)
    private AttendanceCorrectionType type;

    @Column(nullable = false)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceCorrectionStatus status;

    protected AttendanceCorrectionEntity() {
    }

    AttendanceCorrectionEntity(AttendanceCorrection correction) {
        this.id = correction.id();
        this.organizationId = correction.organizationId();
        this.employeeId = correction.employeeId();
        this.attendanceId = correction.attendanceId();
        this.type = correction.type();
        this.details = correction.details();
        this.status = correction.status();
    }

    AttendanceCorrection toRecord() {
        return new AttendanceCorrection(id, organizationId, employeeId, attendanceId, type, details, status);
    }
}

interface AttendanceCorrectionRepository extends JpaRepository<AttendanceCorrectionEntity, UUID> {
}

@Component
@Profile("!test")
class JpaAttendanceCorrectionStore implements AttendanceCorrectionStore {

    private final AttendanceCorrectionRepository repository;

    JpaAttendanceCorrectionStore(AttendanceCorrectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AttendanceCorrection> findAll() {
        return repository.findAll().stream().map(AttendanceCorrectionEntity::toRecord).toList();
    }

    @Override
    public Optional<AttendanceCorrection> findById(UUID id) {
        return repository.findById(id).map(AttendanceCorrectionEntity::toRecord);
    }

    @Override
    public AttendanceCorrection save(AttendanceCorrection correction) {
        repository.save(new AttendanceCorrectionEntity(correction));
        return correction;
    }
}