package org.example.simu.auth.exception;

/**
 * OAuth state is missing, expired or does not match.
 */
public class InvalidOAuthStateException extends RuntimeException {
    public InvalidOAuthStateException(String message) {
        super(message);
    }
}
