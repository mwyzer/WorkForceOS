package com.workforceos.schedule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryShiftTemplateStore implements ShiftTemplateStore {

    private final ConcurrentMap<UUID, ShiftTemplate> shifts = new ConcurrentHashMap<>();

    @Override
    public List<ShiftTemplate> findAll() {
        return List.copyOf(shifts.values());
    }

    @Override
    public Optional<ShiftTemplate> findById(UUID id) {
        return Optional.ofNullable(shifts.get(id));
    }

    @Override
    public ShiftTemplate save(ShiftTemplate shift) {
        shifts.put(shift.id(), shift);
        return shift;
    }
}