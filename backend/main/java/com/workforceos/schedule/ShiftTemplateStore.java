package com.workforceos.schedule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShiftTemplateStore {

    List<ShiftTemplate> findAll();

    Optional<ShiftTemplate> findById(UUID id);

    ShiftTemplate save(ShiftTemplate shift);
}