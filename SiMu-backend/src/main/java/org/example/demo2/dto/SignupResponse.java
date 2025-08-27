package org.example.demo2.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response for successful user registration")
public record SignupResponse(
    @Schema(description = "Message indicating the result of the registration", example = "User registered successfully")
    String message,
    @Schema(description = "JWT token for authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String token,
    @Schema(description = "Unique identifier of the registered user", example = "1")
    Long userId
) {}
