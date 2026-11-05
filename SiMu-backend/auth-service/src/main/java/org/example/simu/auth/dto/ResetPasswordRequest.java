package org.example.simu.auth.dto;

import jakarta.validation.constraints.NotBlank;
import org.example.simu.auth.validation.StrongPassword;

public record ResetPasswordRequest(
        @NotBlank(message = "Token must not be blank") String token,
        @NotBlank(message = "Password must not be blank") @StrongPassword String newPassword
) {
}
