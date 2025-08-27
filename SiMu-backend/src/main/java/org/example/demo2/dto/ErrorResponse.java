package org.example.demo2.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response for failed requests")
public record ErrorResponse(
    @Schema(description = "Error message describing the issue", example = "Username already exists")
    String message,
    @Schema(description = "Error code for the issue", example = "USER_ALREADY_EXISTS")
    String errorCode
) {}