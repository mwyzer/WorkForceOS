package com.workforceos.schedule;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "shift_templates")
class ShiftTemplateEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private boolean overnight;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private String breaks;

    protected ShiftTemplateEntity() {
    }

    ShiftTemplateEntity(ShiftTemplate shift, String breaksJson) {
        this.id = shift.id();
        this.organizationId = shift.organizationId();
        this.name = shift.name();
        this.startTime = shift.startTime();
        this.endTime = shift.endTime();
        this.overnight = shift.overnight();
        this.active = shift.active();
        this.breaks = breaksJson;
    }

    ShiftTemplate toRecord(BreakPeriodJson breaksJson) {
        return new ShiftTemplate(id, organizationId, name, startTime, endTime, overnight,
                breaksJson.toBreaks(breaks), active);
    }
}

interface ShiftTemplateRepository extends JpaRepository<ShiftTemplateEntity, UUID> {
}

@Component
@Profile("!test")
class JpaShiftTemplateStore implements ShiftTemplateStore {

    private final ShiftTemplateRepository repository;
    private final BreakPeriodJson breaks;

    JpaShiftTemplateStore(ShiftTemplateRepository repository, BreakPeriodJson breaks) {
        this.repository = repository;
        this.breaks = breaks;
    }

    @Override
    public List<ShiftTemplate> findAll() {
        return repository.findAll().stream().map(entity -> entity.toRecord(breaks)).toList();
    }

    @Override
    public Optional<ShiftTemplate> findById(UUID id) {
        return repository.findById(id).map(entity -> entity.toRecord(breaks));
    }

    @Override
    public ShiftTemplate save(ShiftTemplate shift) {
        repository.save(new ShiftTemplateEntity(shift, breaks.toJson(shift.breaks())));
        return shift;
    }
}

/**
 * Serializes/deserializes shift template break periods to the TEXT column. Uses the
 * Spring-configured {@link ObjectMapper} so Java 8 date/time support (jsr310) is active.
 */
@Component
class BreakPeriodJson {

    private final ObjectMapper mapper;

    BreakPeriodJson(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    String toJson(List<BreakPeriod> breakPeriods) {
        try {
            return mapper.writeValueAsString(breakPeriods);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize shift template breaks", ex);
        }
    }

    List<BreakPeriod> toBreaks(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<List<BreakPeriod>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize shift template breaks", ex);
        }
    }
}