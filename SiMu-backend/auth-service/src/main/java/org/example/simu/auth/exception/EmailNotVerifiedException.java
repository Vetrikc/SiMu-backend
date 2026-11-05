package org.example.simu.auth.exception;

/**
 * Google account e-mail is not verified.
 */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
