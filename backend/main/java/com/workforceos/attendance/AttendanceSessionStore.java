package com.workforceos.attendance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceSessionStore {

    Optional<AttendanceSession> findActiveByEmployeeId(UUID employeeId);

    List<AttendanceSession> findByEmployeeId(UUID employeeId);

    List<AttendanceSession> findAll();

    AttendanceSession save(AttendanceSession session);
}