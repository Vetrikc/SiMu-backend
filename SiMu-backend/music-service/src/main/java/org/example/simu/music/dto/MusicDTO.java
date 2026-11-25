package org.example.simu.music.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record MusicDTO(
        Long id,
        @NotBlank(message = "Name must not be blank") String name,
        @NotBlank(message = "Artist must not be blank") String artist,
        @NotBlank(message = "URL must not be blank") String url
) implements Serializable {
}