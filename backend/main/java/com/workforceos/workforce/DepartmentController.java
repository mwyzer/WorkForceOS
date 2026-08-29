package com.workforceos.workforce;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/departments")
    public List<Department> findAll() {
        return departmentService.findAll();
    }

    @GetMapping("/departments/{id}")
    public Department findById(@PathVariable UUID id) {
        return departmentService.findById(id);
    }

    @PostMapping("/departments")
    public ResponseEntity<Department> create(@RequestBody DepartmentRequest request) {
        Department department = departmentService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/departments/" + department.id())).body(department);
    }

    @DeleteMapping("/departments/{id}")
    public Department deactivate(@PathVariable UUID id) {
        return departmentService.deactivate(id);
    }
}
