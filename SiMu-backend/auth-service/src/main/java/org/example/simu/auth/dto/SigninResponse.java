package org.example.simu.auth.dto;

public record SigninResponse(String token, String refreshToken, long expiresIn, long refreshExpiresIn) {
}
