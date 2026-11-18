package com.workforceos.authentication;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody AuthLoginRequest request) {
        return Map.of("accessToken", authService.login(request), "tokenType", "Bearer");
    }

    @PostMapping("/register-tenant")
    public ResponseEntity<TenantRegistrationResponse> registerTenant(@RequestBody TenantRegistrationRequest request) {
        TenantRegistrationResponse response = authService.registerTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
