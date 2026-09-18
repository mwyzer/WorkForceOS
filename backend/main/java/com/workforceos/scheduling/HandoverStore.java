package com.workforceos.scheduling;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HandoverStore {

    List<Handover> findAll();

    Optional<Handover> findById(UUID id);

    Handover save(Handover handover);
}