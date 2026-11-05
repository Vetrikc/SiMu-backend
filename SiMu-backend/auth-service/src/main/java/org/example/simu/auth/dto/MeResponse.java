package org.example.simu.auth.dto;

import java.time.Instant;
import java.util.Set;

/**
 * {@code passwordLoginEnabled} is false for accounts created through Google: they have no password
 * at all, so the frontend must not offer a password form for them.
 */
public record MeResponse(Long id, String username, String email, Set<String> roles,
                         boolean emailVerified, boolean passwordLoginEnabled, Instant createdAt) {
}
