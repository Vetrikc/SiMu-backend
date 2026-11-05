package org.example.simu.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by auth-service after a successful signup.
 * user-service projects it into its own {@code user_profiles} table.
 */
public record UserRegisteredEvent(UUID eventId, Instant occurredAt, Long userId, String username, String email) {
}
