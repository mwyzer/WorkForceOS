package com.workforceos.schedule;

import java.time.OffsetDateTime;
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
@Table(name = "rosters")
class RosterEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RosterStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private boolean active;

    protected RosterEntity() {
    }

    RosterEntity(Roster roster) {
        this.id = roster.id();
        this.organizationId = roster.organizationId();
        this.name = roster.name();
        this.status = roster.status();
        this.createdAt = roster.createdAt();
        this.active = roster.active();
    }

    Roster toRecord() {
        return new Roster(id, organizationId, name, status, createdAt, active);
    }
}

interface RosterRepository extends JpaRepository<RosterEntity, UUID> {
}

@Component
@Profile("!test")
class JpaRosterStore implements RosterStore {

    private final RosterRepository repository;

    JpaRosterStore(RosterRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Roster> findAll() {
        return repository.findAll().stream().map(RosterEntity::toRecord).toList();
    }

    @Override
    public Optional<Roster> findById(UUID id) {
        return repository.findById(id).map(RosterEntity::toRecord);
    }

    @Override
    public Roster save(Roster roster) {
        repository.save(new RosterEntity(roster));
        return roster;
    }
}