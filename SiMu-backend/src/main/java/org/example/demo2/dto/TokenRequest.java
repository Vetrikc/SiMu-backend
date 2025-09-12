package org.example.demo2.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for refresh token request.
 */
public record TokenRequest(
        @NotBlank(message = "Refresh token must not be blank") String refreshToken) {}