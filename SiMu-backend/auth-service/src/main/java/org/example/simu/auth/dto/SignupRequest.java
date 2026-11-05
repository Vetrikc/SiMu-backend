package org.example.simu.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.simu.auth.validation.StrongPassword;

public record SignupRequest(
        @NotBlank(message = "Username must not be blank")
        @Size(min = 2, max = 20, message = "Username must be 2-20 characters long")
        String username,

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @NotBlank(message = "Password must not be blank")
        @StrongPassword
        String password
) {
}
