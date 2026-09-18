package com.workforceos.authentication;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_accounts")
class UserAccountEntity {

    @Id
    private String username;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_account_roles", joinColumns = @JoinColumn(name = "username"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    @Column(nullable = false)
    private boolean active;

    protected UserAccountEntity() {
    }

    UserAccountEntity(String username, UUID organizationId, String passwordHash, Set<String> roles, boolean active) {
        this.username = username;
        this.organizationId = organizationId;
        this.passwordHash = passwordHash;
        this.roles = new HashSet<>(roles);
        this.active = active;
    }

    UserAccount toRecord() {
        return new UserAccount(username, organizationId, passwordHash, new LinkedHashSet<>(roles), active);
    }
}
