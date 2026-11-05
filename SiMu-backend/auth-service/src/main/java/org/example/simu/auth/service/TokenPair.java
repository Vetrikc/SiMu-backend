package org.example.simu.auth.service;

/**
 * Freshly issued token pair. The refresh token is returned only here, only the hash is stored.
 */
public record TokenPair(String accessToken, String refreshToken, long expiresIn, long refreshExpiresIn) {
}
