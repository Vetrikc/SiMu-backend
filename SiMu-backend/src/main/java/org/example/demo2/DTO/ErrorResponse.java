package org.example.demo2.DTO;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

/**
 * DTO for error responses.
 */
@Getter
@Setter
public class ErrorResponse {
    private String message;

    private String code;


    public ErrorResponse(String message, String code) {
        this.message = message;
        this.code = code;
    }
}