package org.example.simu.auth.controller;

import jakarta.validation.Valid;
import org.example.simu.auth.dto.ErrorResponse;
import org.example.simu.auth.dto.RoleUpdateRequest;
import org.example.simu.auth.dto.UserDetailsResponse;
import org.example.simu.auth.exception.UserNotFoundException;
import org.example.simu.auth.service.UserDirectory;
import org.example.simu.auth.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service API. The gateway does not route it and the internal API key filter requires
 * {@code X-Internal-Token}, so it is not reachable from the internet.
 */
@RestController
@RequestMapping("/api/v1/internal/users")
public class InternalUserController {

    private final UserDirectory userDirectory;
    private final UserService userService;

    public InternalUserController(UserDirectory userDirectory, UserService userService) {
        this.userDirectory = userDirectory;
        this.userService = userService;
    }

    @GetMapping(value = "/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDetailsResponse> getUser(@PathVariable String username) {
        return ResponseEntity.ok(userDirectory.getUserDetails(username));
    }

    @PatchMapping(value = "/{id}/roles", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDetailsResponse> updateRoles(@PathVariable Long id,
                                                           @Valid @RequestBody RoleUpdateRequest request) {
        var user = userService.updateRoles(id, request.roles());
        return ResponseEntity.ok(new UserDetailsResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getRoles(), user.isEmailVerified()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), "USER_NOT_FOUND"));
    }
}
