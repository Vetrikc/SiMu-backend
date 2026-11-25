package org.example.simu.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.simu.auth.client.PlaylistClient;
import org.example.simu.auth.dto.ErrorResponse;
import org.example.simu.auth.dto.SigninRequest;
import org.example.simu.auth.dto.SigninResponse;
import org.example.simu.auth.dto.SignupRequest;
import org.example.simu.auth.dto.SignupResponse;
import org.example.simu.auth.entity.User;
import org.example.simu.auth.exception.UserAlreadyExistsException;
import org.example.simu.auth.service.AccountService;
import org.example.simu.auth.service.LoginAuditService;
import org.example.simu.auth.service.TokenPair;
import org.example.simu.auth.service.TokenService;
import org.example.simu.auth.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registration and sign-in. The response keeps the {@code token} field the frontend already reads and
 * adds the refresh half of the pair.
 */
@Tag(name = "Authentication", description = "Registration, sign-in, tokens")
@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class SecurityController {

    private static final Logger log = LoggerFactory.getLogger(SecurityController.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final AccountService accountService;
    private final LoginAuditService loginAuditService;
    private final PlaylistClient playlistClient;

    public SecurityController(UserService userService,
                              PasswordEncoder passwordEncoder,
                              AuthenticationManager authenticationManager,
                              TokenService tokenService,
                              AccountService accountService,
                              LoginAuditService loginAuditService,
                              PlaylistClient playlistClient) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.accountService = accountService;
        this.loginAuditService = loginAuditService;
        this.playlistClient = playlistClient;
    }

    @Operation(summary = "Register a new user")
    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest signupRequest,
                                                 HttpServletRequest httpRequest) {
        log.debug("Signup request for username: {}", signupRequest.username());

        if (userService.existsByUsername(signupRequest.username())) {
            throw new UserAlreadyExistsException("Username already exists: " + signupRequest.username());
        }
        if (userService.existsByEmail(signupRequest.email())) {
            throw new UserAlreadyExistsException("Email already exists: " + signupRequest.email());
        }

        User user = userService.createUser(signupRequest.username(), signupRequest.email(),
                passwordEncoder.encode(signupRequest.password()));

        // best effort, must not break the registration
        accountService.requestEmailVerification(user);
        playlistClient.createDefaultPlaylist(user.getId());

        TokenPair pair = tokenService.issue(user, clientIp(httpRequest), httpRequest.getHeader("User-Agent"));
        loginAuditService.record(user.getId(), user.getUsername(), true, "SIGNUP", httpRequest);

        log.debug("User registered successfully with ID: {}", user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SignupResponse("User registered successfully", pair.accessToken(), pair.refreshToken(),
                        pair.expiresIn(), pair.refreshExpiresIn(), user.getId()));
    }

    @Operation(summary = "Authenticate a user")
    @PostMapping(value = "/signin", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SigninResponse> signin(@Valid @RequestBody SigninRequest signinRequest,
                                                 HttpServletRequest httpRequest) {
        log.debug("Signin request for username: {}", signinRequest.username());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(signinRequest.username(), signinRequest.password()));
            User user = userService.findByUsername(authentication.getName());

            TokenPair pair = tokenService.issue(user, clientIp(httpRequest), httpRequest.getHeader("User-Agent"));
            loginAuditService.record(user.getId(), user.getUsername(), true, null, httpRequest);

            log.debug("User signed in successfully: {}", signinRequest.username());
            return ResponseEntity.ok(new SigninResponse(pair.accessToken(), pair.refreshToken(),
                    pair.expiresIn(), pair.refreshExpiresIn()));
        } catch (BadCredentialsException e) {
            loginAuditService.record(null, signinRequest.username(), false, "INVALID_CREDENTIALS", httpRequest);
            throw e;
        }
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("User registration failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), "USER_ALREADY_EXISTS"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication failed");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Invalid username or password", "INVALID_CREDENTIALS"));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}