package org.example.demo2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request for user authentication")
public record SigninRequest(
    @Schema(description = "Username of the user", example = "john_doe")
    @NotBlank(message = "Username must not be blank")
    String username,

    @NotBlank(message = "Password must not be blank")
    @Schema(description = "Password of the user", example = "password123")
    String password
) {}