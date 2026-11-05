package org.example.simu.auth.exception;

/**
 * Google ID token is invalid.
 */
public class InvalidGoogleTokenException extends RuntimeException {
    public InvalidGoogleTokenException(String message) {
        super(message);
    }
}
