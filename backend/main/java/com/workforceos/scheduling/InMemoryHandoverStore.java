package com.workforceos.scheduling;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryHandoverStore implements HandoverStore {

    private final ConcurrentMap<UUID, Handover> handovers = new ConcurrentHashMap<>();

    @Override
    public List<Handover> findAll() {
        return List.copyOf(handovers.values());
    }

    @Override
    public Optional<Handover> findById(UUID id) {
        return Optional.ofNullable(handovers.get(id));
    }

    @Override
    public Handover save(Handover handover) {
        handovers.put(handover.id(), handover);
        return handover;
    }
}