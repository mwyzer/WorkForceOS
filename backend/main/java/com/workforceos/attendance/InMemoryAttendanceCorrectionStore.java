package com.workforceos.attendance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryAttendanceCorrectionStore implements AttendanceCorrectionStore {

    private final ConcurrentMap<UUID, AttendanceCorrection> corrections = new ConcurrentHashMap<>();

    @Override
    public List<AttendanceCorrection> findAll() {
        return List.copyOf(corrections.values());
    }

    @Override
    public Optional<AttendanceCorrection> findById(UUID id) {
        return Optional.ofNullable(corrections.get(id));
    }

    @Override
    public AttendanceCorrection save(AttendanceCorrection correction) {
        corrections.put(correction.id(), correction);
        return correction;
    }
}