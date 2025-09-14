package org.example.demo2.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePlaylistRequest(@NotNull String name, String description) {}