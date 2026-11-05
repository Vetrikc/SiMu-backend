package org.example.simu.auth.controller;

import org.example.simu.auth.dto.ErrorResponse;
import org.example.simu.auth.exception.EmailNotVerifiedException;
import org.example.simu.auth.exception.InvalidGoogleTokenException;
import org.example.simu.auth.exception.InvalidOAuthStateException;
import org.example.simu.auth.exception.InvalidTicketException;
import org.example.simu.auth.exception.OAuthNotConfiguredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps the OAuth failures to meaningful statuses. The messages never contain the code, the
 * verifier or the ID token.
 */
@RestControllerAdvice
public class OAuthExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuthExceptionHandler.class);

    @ExceptionHandler(OAuthNotConfiguredException.class)
    public ResponseEntity<ErrorResponse> notConfigured(OAuthNotConfiguredException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(ex.getMessage(), "OAUTH_NOT_CONFIGURED"));
    }

    @ExceptionHandler(InvalidOAuthStateException.class)
    public ResponseEntity<ErrorResponse> invalidState(InvalidOAuthStateException ex) {
        log.warn("OAuth state rejected: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), "INVALID_OAUTH_STATE"));
    }

    @ExceptionHandler(InvalidGoogleTokenException.class)
    public ResponseEntity<ErrorResponse> invalidToken(InvalidGoogleTokenException ex) {
        log.warn("Google token rejected: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ex.getMessage(), "INVALID_GOOGLE_TOKEN"));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> emailNotVerified(EmailNotVerifiedException ex) {
        log.warn("Google login rejected: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage(), "OAUTH_EMAIL_NOT_VERIFIED"));
    }

    @ExceptionHandler(InvalidTicketException.class)
    public ResponseEntity<ErrorResponse> invalidTicket(InvalidTicketException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), "INVALID_TICKET"));
    }
}
