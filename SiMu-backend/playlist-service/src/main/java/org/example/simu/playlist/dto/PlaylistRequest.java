package org.example.simu.playlist.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Payload used by {@code user-service} when a playlist is created from the user's own page.
 */
public record PlaylistRequest(@NotNull String name, List<String> tracks) {
}
