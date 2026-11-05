package org.example.simu.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.simu.auth.dto.ErrorResponse;
import org.example.simu.auth.dto.ForgotPasswordRequest;
import org.example.simu.auth.dto.MeResponse;
import org.example.simu.auth.dto.ResetPasswordRequest;
import org.example.simu.auth.dto.UserDetailsResponse;
import org.example.simu.auth.dto.VerifyEmailRequest;
import org.example.simu.auth.exception.InvalidEmailTokenException;
import org.example.simu.auth.service.AccountService;
import org.example.simu.auth.service.UserDirectory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Account self-service: current profile, e-mail verification and password reset.
 */
@Tag(name = "Account", description = "Profile, e-mail verification, password reset")
@RestController
@RequestMapping("/api/v1/auth")
public class AccountController {

    private final UserDirectory userDirectory;
    private final AccountService accountService;

    public AccountController(UserDirectory userDirectory, AccountService accountService) {
        this.userDirectory = userDirectory;
        this.accountService = accountService;
    }

    @Operation(summary = "Current user profile")
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        UserDetailsResponse user = userDirectory.getUserDetails(authentication.getName());
        return ResponseEntity.ok(new MeResponse(user.id(), user.username(), user.email(), user.roles(),
                user.emailVerified(), user.passwordLoginEnabled(), Instant.now()));
    }

    @Operation(summary = "Confirm the e-mail address")
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        accountService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Request a password reset link")
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        accountService.requestPasswordReset(request.email());
        // always 202: the response must not reveal whether the address exists
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Set a new password with a reset token")
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(InvalidEmailTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidEmailTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), "INVALID_TOKEN"));
    }
}