package org.example.simu.auth.exception;

/**
 * Google login is not configured.
 */
public class OAuthNotConfiguredException extends RuntimeException {
    public OAuthNotConfiguredException(String message) {
        super(message);
    }
}
