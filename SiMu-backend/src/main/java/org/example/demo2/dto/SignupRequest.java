package org.example.demo2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request for user registration")
public record SignupRequest(
    @Schema(description = "Username for the new user", example = "john_doe")
    @NotBlank(message = "Username must not be blank")
    String username,
    @Schema(description = "Email address of the user", example = "john.doe@example.com")
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be valid")
    String email,
    @Schema(description = "Password for the user account", example = "password123")
    @NotBlank(message = "Password must not be blank")
    String password
) {}