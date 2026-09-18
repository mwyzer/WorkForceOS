package com.workforceos.schedule;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryRosterAssignmentStore implements RosterAssignmentStore {

    private final ConcurrentMap<UUID, ConcurrentMap<UUID, RosterAssignment>> assignmentsByRoster;

    public InMemoryRosterAssignmentStore() {
        this.assignmentsByRoster = new ConcurrentHashMap<>();
    }

    @Override
    public List<RosterAssignment> findAll() {
        return assignmentsByRoster.values().stream()
                .flatMap(assignments -> assignments.values().stream())
                .toList();
    }

    @Override
    public List<RosterAssignment> findByRosterId(UUID rosterId) {
        ConcurrentMap<UUID, RosterAssignment> assignments = assignmentsByRoster.get(rosterId);
        return assignments == null ? List.of() : List.copyOf(assignments.values());
    }

    @Override
    public RosterAssignment save(RosterAssignment assignment) {
        assignmentsByRoster.computeIfAbsent(assignment.rosterId(), ignored -> new ConcurrentHashMap<>())
                .put(assignment.id(), assignment);
        return assignment;
    }

    @Override
    public void delete(UUID id) {
        assignmentsByRoster.values()
                .forEach(assignments -> assignments.remove(id));
    }
}