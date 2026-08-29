package com.workforceos.workforce;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, UUID> {
}
