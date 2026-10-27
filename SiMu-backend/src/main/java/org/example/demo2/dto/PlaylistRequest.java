package org.example.demo2.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PlaylistRequest(@NotNull String name, List<String> tracks) {}