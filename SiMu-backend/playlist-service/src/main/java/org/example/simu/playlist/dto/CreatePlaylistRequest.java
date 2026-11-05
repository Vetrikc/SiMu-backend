package org.example.simu.playlist.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePlaylistRequest(@NotNull String name, String description) {
}
