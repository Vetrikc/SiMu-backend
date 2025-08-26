package org.example.demo2.controller;

import jakarta.validation.Valid;
import org.example.demo2.DTO.ErrorResponse;
import org.example.demo2.DTO.SigninRequest;
import org.example.demo2.DTO.SignupRequest;
import org.example.demo2.security.JwtCore;
import org.example.demo2.services.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for handling authentication and registration.
 */
@RestController
@RequestMapping("/api/auth")
public class SecurityController {
    private static final Logger logger = LoggerFactory.getLogger(SecurityController.class);

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtCore jwtCore;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        logger.debug("Signup request for username: {}", signupRequest.getUsername());
        if (userService.existsByUsername(signupRequest.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Username already exists"));
        }
        if (userService.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Email already exists"));
        }
        userService.createUser(signupRequest.getUsername(), signupRequest.getEmail(), passwordEncoder.encode(signupRequest.getPassword()));
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(signupRequest.getUsername(), signupRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtCore.generateToken(authentication);
        logger.info("User registered successfully: {}", signupRequest.getUsername());
        return ResponseEntity.ok(Map.of("message", "User registered successfully", "token", jwt));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@Valid @RequestBody SigninRequest signinRequest) {
        logger.debug("Signin request for username: {}", signinRequest.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(signinRequest.getUsername(), signinRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtCore.generateToken(authentication);
            logger.info("User signed in successfully: {}", signinRequest.getUsername());
            return ResponseEntity.ok(Map.of("token", jwt));
        } catch (BadCredentialsException e) {
            logger.warn("Invalid credentials for username: {}", signinRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invallid username or password", "INVALID_CREDENTIALS"));
        }
    }
}