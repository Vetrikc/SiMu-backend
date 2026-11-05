package org.example.simu.auth.dto;

import java.time.Instant;
import java.util.Set;

public record MeResponse(Long id, String username, String email, Set<String> roles,
                         boolean emailVerified, Instant createdAt) {
}
