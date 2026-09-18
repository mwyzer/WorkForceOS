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
public class InMemoryRosterStore implements RosterStore {

    private final ConcurrentMap<UUID, Roster> rosters = new ConcurrentHashMap<>();

    @Override
    public List<Roster> findAll() {
        return List.copyOf(rosters.values());
    }

    @Override
    public Optional<Roster> findById(UUID id) {
        return Optional.ofNullable(rosters.get(id));
    }

    @Override
    public Roster save(Roster roster) {
        rosters.put(roster.id(), roster);
        return roster;
    }
}