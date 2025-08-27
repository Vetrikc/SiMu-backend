package org.example.demo2.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object for Music entity.
 */
@Getter
@Setter
public class MusicDTO {
    private Long id;

    @NotBlank(message = "Name must not be blank")
    private String name;

    @NotBlank(message = "Artist must not be blank")
    private String artist;

    @NotBlank(message = "URL must not be blank")
    private String url;
}