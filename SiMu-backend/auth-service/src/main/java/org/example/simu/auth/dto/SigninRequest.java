package org.example.simu.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SigninRequest(
        @NotBlank(message = "Username must not be blank") String username,
        @NotBlank(message = "Password must not be blank") String password
) {
}
