package org.example.demo2.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for token refresh response.
 */
public record TokenResponse(
        @NotBlank String accessToken,
        @NotBlank String refreshToken) {
}