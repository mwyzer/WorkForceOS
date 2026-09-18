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
public class InMemoryShiftSwapRequestStore implements ShiftSwapRequestStore {

    private final ConcurrentMap<UUID, ShiftSwapRequest> shiftSwapRequests = new ConcurrentHashMap<>();

    @Override
    public List<ShiftSwapRequest> findAll() {
        return List.copyOf(shiftSwapRequests.values());
    }

    @Override
    public Optional<ShiftSwapRequest> findById(UUID id) {
        return Optional.ofNullable(shiftSwapRequests.get(id));
    }

    @Override
    public ShiftSwapRequest save(ShiftSwapRequest request) {
        shiftSwapRequests.put(request.id(), request);
        return request;
    }
}