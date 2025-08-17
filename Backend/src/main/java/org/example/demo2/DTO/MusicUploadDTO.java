package org.example.demo2.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object for Music entity.
 */
@Getter
@Setter
public class MusicUploadDTO {

    @NotBlank(message = "Name must not be blank")
    private String name;

    @NotBlank(message = "Artist must not be blank")
    private String artist;
}