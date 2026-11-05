package org.example.simu.music.dto;

import jakarta.validation.constraints.NotNull;

public record MusicUploadDTO(@NotNull String name, @NotNull String artist) {
}
