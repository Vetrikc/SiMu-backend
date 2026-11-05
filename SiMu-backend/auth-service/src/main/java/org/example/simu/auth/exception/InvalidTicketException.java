package org.example.simu.auth.exception;

/**
 * Login ticket is unknown or already used.
 */
public class InvalidTicketException extends RuntimeException {
    public InvalidTicketException(String message) {
        super(message);
    }
}
