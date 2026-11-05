package org.example.simu.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank(message = "Username must not be blank") String username,
        @NotBlank(message = "Email must not be blank") @Email(message = "Email must be valid") String email,
        @NotBlank(message = "Password must not be blank") String password
) {
}
