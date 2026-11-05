package org.example.simu.auth.controller;

import org.example.simu.auth.dto.ErrorResponse;
import org.example.simu.auth.dto.UserDetailsResponse;
import org.example.simu.auth.exception.UserNotFoundException;
import org.example.simu.auth.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service API. It is not routed by the gateway, so it is unreachable from the internet.
 */
@RestController
@RequestMapping("/api/v1/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = "/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDetailsResponse> getUser(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(user -> new UserDetailsResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRoles()))
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), "USER_NOT_FOUND"));
    }
}
