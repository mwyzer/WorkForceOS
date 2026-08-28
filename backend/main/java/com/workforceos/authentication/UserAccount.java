package com.workforceos.authentication;

import java.util.Set;

record UserAccount(String username, String passwordHash, Set<String> roles, boolean active) {
}
