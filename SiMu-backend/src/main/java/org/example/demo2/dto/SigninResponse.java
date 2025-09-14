package org.example.demo2.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response for successful user authentication")
public record SigninResponse(
    @Schema(description = "JWT token for authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String token
) {}