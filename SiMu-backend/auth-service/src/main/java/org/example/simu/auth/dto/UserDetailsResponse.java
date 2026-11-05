package org.example.simu.auth.dto;

import java.util.Set;

/**
 * Payload of the internal endpoint used by {@code user-service} to resolve a username.
 */
public record UserDetailsResponse(Long id, String username, String email, Set<String> roles) {
}
