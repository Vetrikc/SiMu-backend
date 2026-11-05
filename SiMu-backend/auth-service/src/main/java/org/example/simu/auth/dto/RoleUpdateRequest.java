package org.example.simu.auth.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record RoleUpdateRequest(@NotEmpty(message = "At least one role is required") Set<String> roles) {
}
