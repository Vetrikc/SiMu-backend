package org.example.simu.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.simu.auth.client.PlaylistClient;
import org.example.simu.auth.dto.ErrorResponse;
import org.example.simu.auth.dto.SigninRequest;
import org.example.simu.auth.dto.SigninResponse;
import org.example.simu.auth.dto.SignupRequest;
import org.example.simu.auth.dto.SignupResponse;
import org.example.simu.auth.entity.User;
import org.example.simu.auth.exception.UserAlreadyExistsException;
import org.example.simu.auth.security.JwtCore;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints. Unchanged contract of the monolith:
 * {@code POST /api/v1/auth/signup} and {@code POST /api/v1/auth/signin}.
 */
@Tag(name = "Authentication", description = "API for user registration and authentication")
@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class SecurityController {

    private static final Logger log = LoggerFactory.getLogger(SecurityController.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtCore jwtCore;
    private final PlaylistClient playlistClient;

    public SecurityController(UserService userService,
                              PasswordEncoder passwordEncoder,
                              AuthenticationManager authenticationManager,
                              JwtCore jwtCore,
                              PlaylistClient playlistClient) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtCore = jwtCore;
        this.playlistClient = playlistClient;
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account and returns a JWT token")
    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
        log.info("Signup request for username: {}", signupRequest.username());

        if (userService.existsByUsername(signupRequest.username())) {
            throw new UserAlreadyExistsException("Username already exists: " + signupRequest.username());
        }
        if (userService.existsByEmail(signupRequest.email())) {
            throw new UserAlreadyExistsException("Email already exists: " + signupRequest.email());
        }

        User user = userService.createUser(signupRequest.username(), signupRequest.email(),
                passwordEncoder.encode(signupRequest.password()));

        // Replaces the in-process creation of the "Favorites" playlist from the monolith.
        playlistClient.createDefaultPlaylist(user.getId());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(signupRequest.username(), signupRequest.password()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtCore.generateToken(authentication);

        log.info("User registered successfully with ID: {}", user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SignupResponse("User registered successfully", jwt, user.getId()));
    }

    @Operation(summary = "Authenticate a user", description = "Authenticates a user and returns a JWT token")
    @PostMapping(value = "/signin", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SigninResponse> signin(@Valid @RequestBody SigninRequest signinRequest) {
        log.info("Signin request for username: {}", signinRequest.username());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(signinRequest.username(), signinRequest.password()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtCore.generateToken(authentication);
        log.info("User signed in successfully: {}", signinRequest.username());
        return ResponseEntity.ok(new SigninResponse(jwt));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("User registration failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), "USER_ALREADY_EXISTS"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Invalid username or password", "INVALID_CREDENTIALS"));
    }
}
