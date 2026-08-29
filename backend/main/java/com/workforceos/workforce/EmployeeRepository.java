package com.workforceos.workforce;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface EmployeeRepository extends JpaRepository<EmployeeEntity, UUID> {

    boolean existsByEmployeeNumberIgnoreCase(String employeeNumber);
}
