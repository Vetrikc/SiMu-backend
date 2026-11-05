package org.example.simu.auth.dto;

/**
 * {@code token} stays the access token for backward compatibility with the existing frontend;
 * the refresh token and lifetimes are additive fields.
 */
public record SignupResponse(String message, String token, String refreshToken,
                             long expiresIn, long refreshExpiresIn, Long userId) {
}
