package org.example.simu.auth.exception;

public class InvalidEmailTokenException extends RuntimeException {
    public InvalidEmailTokenException(String message) {
        super(message);
    }
}
