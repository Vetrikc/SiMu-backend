package org.example.simu.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.simu.user.client.AuthServiceClient;
import org.example.simu.user.client.PlaylistServiceClient;
import org.example.simu.user.dto.ErrorResponse;
import org.example.simu.user.dto.PlaylistRequest;
import org.example.simu.user.dto.PlaylistResponse;
import org.example.simu.user.dto.UserInfoResponse;
import org.example.simu.user.exception.PlaylistNotFoundException;
import org.example.simu.user.exception.UserNotFoundException;
import org.example.simu.user.security.CurrentUserResolver;
import org.example.simu.user.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Public contract for {@code /api/v1/users}. The paths and payloads of the monolith are preserved;
 * the data comes from the local read model (Kafka projection + Redis cache) and playlist-service.
 */
@Tag(name = "Users", description = "API for managing user information and playlists")
@RestController
@RequestMapping("/api/v1/users")
@Validated
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserProfileService userProfileService;
    private final PlaylistServiceClient playlistServiceClient;
    private final AuthServiceClient authServiceClient;
    private final CurrentUserResolver currentUserResolver;

    public UserController(UserProfileService userProfileService,
                          PlaylistServiceClient playlistServiceClient,
                          AuthServiceClient authServiceClient,
                          CurrentUserResolver currentUserResolver) {
        this.userProfileService = userProfileService;
        this.playlistServiceClient = playlistServiceClient;
        this.authServiceClient = authServiceClient;
        this.currentUserResolver = currentUserResolver;
    }

    @Operation(summary = "Get user information", description = "Returns information about the authenticated user")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserInfoResponse> getUserInfo(Authentication authentication) {
        log.info("Fetching user info for: {}", authentication.getName());
        return ResponseEntity.ok(userProfileService.getProfile(authentication.getName()));
    }

    @Operation(summary = "Delete the current account", description = "Soft-deletes the account and its playlists")
    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAccount(Authentication authentication, HttpServletRequest httpRequest) {
        Long userId = currentUserResolver.resolveUserId(authentication, httpRequest);
        log.info("Deleting account id={}", userId);

        authServiceClient.deleteUser(userId);
        userProfileService.deleteProfileByUserId(userId);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add a new playlist", description = "Creates a new playlist for the authenticated user")
    @PostMapping(value = "/playlists", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PlaylistResponse> addPlaylist(Authentication authentication,
                                                        @Valid @RequestBody PlaylistRequest request,
                                                        HttpServletRequest httpRequest,
                                                        UriComponentsBuilder uriBuilder) {
        Long userId = currentUserResolver.resolveUserId(authentication, httpRequest);
        log.info("Adding playlist for user id: {}", userId);

        PlaylistResponse response = playlistServiceClient.createPlaylist(userId, request);

        URI location = uriBuilder.path("/api/v1/playlists/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get user playlists", description = "Returns a list of playlists for the authenticated user")
    @GetMapping(value = "/playlists", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PlaylistResponse>> getUserPlaylists(Authentication authentication,
                                                                  HttpServletRequest httpRequest) {
        Long userId = currentUserResolver.resolveUserId(authentication, httpRequest);
        return ResponseEntity.ok(playlistServiceClient.getUserPlaylists(userId));
    }

    @GetMapping(value = "/playlists1", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("response");
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), "USER_NOT_FOUND"));
    }

    @ExceptionHandler(PlaylistNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePlaylistNotFound(PlaylistNotFoundException ex) {
        log.warn("Playlist not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), "PLAYLIST_NOT_FOUND"));
    }
}
