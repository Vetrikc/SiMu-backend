package org.example.demo2.dto;

import jakarta.validation.constraints.NotNull;

public record MusicUploadDTO(@NotNull String name, @NotNull String artist) {}