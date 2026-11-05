package org.example.simu.common.event;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Published when the role set of a user changes. Consumers update their projections;
 * auth-service additionally revokes the user's refresh tokens, so the next access token
 * already carries the new claims.
 */
public record RoleChangedEvent(UUID eventId, Instant occurredAt, Long userId, Set<String> roles) {
}
