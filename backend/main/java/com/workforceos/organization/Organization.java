package com.workforceos.organization;

import java.time.OffsetDateTime;
import java.util.UUID;

record Organization(UUID id, String name, String timezone, boolean active, OffsetDateTime createdAt) {
}