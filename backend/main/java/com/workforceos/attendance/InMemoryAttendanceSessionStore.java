package com.workforceos.attendance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryAttendanceSessionStore implements AttendanceSessionStore {

    private final ConcurrentMap<UUID, AttendanceSession> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, List<AttendanceSession>> employeeHistory = new ConcurrentHashMap<>();

    @Override
    public Optional<AttendanceSession> findActiveByEmployeeId(UUID employeeId) {
        return Optional.ofNullable(activeSessions.get(employeeId));
    }

    @Override
    public List<AttendanceSession> findByEmployeeId(UUID employeeId) {
        return List.copyOf(employeeHistory.getOrDefault(employeeId, List.of()));
    }

    @Override
    public List<AttendanceSession> findAll() {
        Map<UUID, AttendanceSession> byId = new LinkedHashMap<>();
        employeeHistory.values().stream()
                .flatMap(List::stream)
                .forEach(session -> byId.put(session.id(), session));
        activeSessions.values().forEach(session -> byId.put(session.id(), session));
        return List.copyOf(byId.values());
    }

    @Override
    public AttendanceSession save(AttendanceSession session) {
        if (session.active()) {
            activeSessions.put(session.employeeId(), session);
        } else {
            activeSessions.remove(session.employeeId());
        }
        employeeHistory.computeIfAbsent(session.employeeId(), ignored -> new ArrayList<>()).add(session);
        return session;
    }
}