package org.example.simu.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when an account is soft-deleted: user-service deletes its projection,
 * playlist-service removes the user's library links.
 */
public record UserDeletedEvent(UUID eventId, Instant occurredAt, Long userId) {
}
