package com.workforceos.scheduling;

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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "handovers")
class HandoverEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HandoverStatus status;

    @Column(nullable = false)
    private String items;

    protected HandoverEntity() {
    }

    HandoverEntity(Handover handover, String itemsJson) {
        this.id = handover.id();
        this.organizationId = handover.organizationId();
        this.employeeId = handover.employeeId();
        this.status = handover.status();
        this.items = itemsJson;
    }

    Handover toRecord() {
        return new Handover(id, organizationId, employeeId, HandoverItemJson.toItems(items), status);
    }
}

interface HandoverRepository extends JpaRepository<HandoverEntity, UUID> {
}

@Component
@Profile("!test")
class JpaHandoverStore implements HandoverStore {

    private final HandoverRepository repository;

    JpaHandoverStore(HandoverRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Handover> findAll() {
        return repository.findAll().stream().map(HandoverEntity::toRecord).toList();
    }

    @Override
    public Optional<Handover> findById(UUID id) {
        return repository.findById(id).map(HandoverEntity::toRecord);
    }

    @Override
    public Handover save(Handover handover) {
        repository.save(new HandoverEntity(handover, HandoverItemJson.toJson(handover.items())));
        return handover;
    }
}

final class HandoverItemJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HandoverItemJson() {
    }

    static String toJson(List<HandoverItem> items) {
        try {
            return MAPPER.writeValueAsString(items);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize handover items", ex);
        }
    }

    static List<HandoverItem> toItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<HandoverItem>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize handover items", ex);
        }
    }
}