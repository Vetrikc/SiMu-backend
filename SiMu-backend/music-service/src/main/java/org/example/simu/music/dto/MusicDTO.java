package org.example.simu.music.dto;

import jakarta.validation.constraints.NotBlank;

public record MusicDTO(
        Long id,
        @NotBlank(message = "Name must not be blank") String name,
        @NotBlank(message = "Artist must not be blank") String artist,
        @NotBlank(message = "URL must not be blank") String url
) {
}
