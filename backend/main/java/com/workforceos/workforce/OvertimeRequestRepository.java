package com.workforceos.workforce;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface OvertimeRequestRepository extends JpaRepository<OvertimeRequestEntity, UUID> {
}
