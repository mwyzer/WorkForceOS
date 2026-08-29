package com.workforceos.workforce;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TeamRepository extends JpaRepository<TeamEntity, UUID> {
}
