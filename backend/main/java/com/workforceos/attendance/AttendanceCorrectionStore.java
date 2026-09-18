package com.workforceos.attendance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceCorrectionStore {

    List<AttendanceCorrection> findAll();

    Optional<AttendanceCorrection> findById(UUID id);

    AttendanceCorrection save(AttendanceCorrection correction);
}