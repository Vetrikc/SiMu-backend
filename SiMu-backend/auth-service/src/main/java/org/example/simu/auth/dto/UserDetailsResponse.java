package org.example.simu.auth.dto;

import java.util.Set;

/**
 * Payload of the internal endpoint used by the other services to resolve a username.
 */
public record UserDetailsResponse(Long id, String username, String email, Set<String> roles,
                                  boolean emailVerified, boolean passwordLoginEnabled) {
}
