package com.workforceos.schedule;

import java.util.List;
import java.util.UUID;

public interface RosterAssignmentStore {

    List<RosterAssignment> findAll();

    List<RosterAssignment> findByRosterId(UUID rosterId);

    RosterAssignment save(RosterAssignment assignment);

    void delete(UUID id);
}