package org.example.simu.playlist.exception;

/**
 * Raised when a request reaches the controller without an identity (no {@code X-User-Id} header
 * and no {@code userId} claim in the token).
 */
public class NotAuthenticatedException extends RuntimeException {

    public NotAuthenticatedException(String message) {
        super(message);
    }
}
