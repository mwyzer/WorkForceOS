package com.workforceos.schedule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RosterStore {

    List<Roster> findAll();

    Optional<Roster> findById(UUID id);

    Roster save(Roster roster);
}