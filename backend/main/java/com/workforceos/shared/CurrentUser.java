package com.workforceos.shared;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.authentication.UserAccount;

@Component
public class CurrentUser {

    private final boolean securityEnabled;

    @Autowired
    public CurrentUser(@Value("${spring.security.enabled:true}") boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public CurrentUser() {
        this(true);
    }

    public String username() {
        UserAccount account = account();
        return account == null ? null : account.username();
    }

    public boolean isAdmin() {
        UserAccount account = account();
        return account != null && account.roles().contains("ADMIN");
    }

    public void requireAdmin() {
        if (!securityEnabled) {
            return;
        }
        if (!isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator role required");
        }
    }

    public UserAccount account() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserAccount userAccount) {
            return userAccount;
        }
        if (principal == null || "anonymousUser".equals(principal)) {
            return null;
        }
        return null;
    }
}