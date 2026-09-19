package com.workforceos.authentication;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workforceos.shared.CurrentUser;

@RestController
@RequestMapping("/api/v1/admin/accounts")
public class AdminController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    public AdminController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<AdminAccountResponse> create(@RequestBody AdminAccountRequest request) {
        currentUser.requireAdmin();
        UserAccount account = authService.provisionAccount(
                request == null ? null : request.username(),
                request == null ? null : request.password(),
                request == null ? null : request.organizationId(),
                request == null ? null : request.roles());
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminAccountResponse.from(account));
    }
}