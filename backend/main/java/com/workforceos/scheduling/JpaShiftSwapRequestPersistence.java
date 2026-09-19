package com.workforceos.scheduling;

import java.time.LocalDate;
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
@Table(name = "shift_swap_requests")
class ShiftSwapRequestEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "requesting_employee_id", nullable = false)
    private UUID requestingEmployeeId;

    @Column(name = "target_employee_id", nullable = false)
    private UUID targetEmployeeId;

    @Column(name = "offered_date", nullable = false)
    private LocalDate offeredDate;

    @Column(name = "requested_date", nullable = false)
    private LocalDate requestedDate;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShiftSwapRequestStatus status;

    protected ShiftSwapRequestEntity() {
    }

    ShiftSwapRequestEntity(ShiftSwapRequest request) {
        this.id = request.id();
        this.organizationId = request.organizationId();
        this.requestingEmployeeId = request.requestingEmployeeId();
        this.targetEmployeeId = request.targetEmployeeId();
        this.offeredDate = request.offeredDate();
        this.requestedDate = request.requestedDate();
        this.reason = request.reason();
        this.status = request.status();
    }

    ShiftSwapRequest toRecord() {
        return new ShiftSwapRequest(id, organizationId, requestingEmployeeId, targetEmployeeId, offeredDate,
                requestedDate, reason,
                status);
    }
}

interface ShiftSwapRequestRepository extends JpaRepository<ShiftSwapRequestEntity, UUID> {
}

@Component
@Profile("!test")
class JpaShiftSwapRequestStore implements ShiftSwapRequestStore {

    private final ShiftSwapRequestRepository repository;

    JpaShiftSwapRequestStore(ShiftSwapRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ShiftSwapRequest> findAll() {
        return repository.findAll().stream().map(ShiftSwapRequestEntity::toRecord).toList();
    }

    @Override
    public Optional<ShiftSwapRequest> findById(UUID id) {
        return repository.findById(id).map(ShiftSwapRequestEntity::toRecord);
    }

    @Override
    public ShiftSwapRequest save(ShiftSwapRequest request) {
        repository.save(new ShiftSwapRequestEntity(request));
        return request;
    }
}