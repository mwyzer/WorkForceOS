package com.workforceos.scheduling;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShiftSwapRequestStore {

    List<ShiftSwapRequest> findAll();

    Optional<ShiftSwapRequest> findById(UUID id);

    ShiftSwapRequest save(ShiftSwapRequest request);
}