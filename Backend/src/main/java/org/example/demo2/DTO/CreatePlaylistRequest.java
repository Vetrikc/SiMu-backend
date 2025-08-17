package org.example.demo2.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for creating a playlist.
 */
@Getter
@Setter
public class CreatePlaylistRequest {
    @NotBlank(message = "Name must not be blank")
    private String name;

    private String description;
}