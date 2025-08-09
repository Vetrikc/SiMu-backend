package org.example.demo2.DTO;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for error responses.
 */
@Getter
@Setter
public class ErrorResponse {
    private String error;

    public ErrorResponse(String error) {
        this.error = error;
    }
}