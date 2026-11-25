package org.example.simu.playlist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.simu.playlist.dto.CreatePlaylistRequest;
import org.example.simu.playlist.dto.ErrorResponse;
import org.example.simu.playlist.dto.PlaylistResponse;
import org.example.simu.playlist.exception.NotAuthenticatedException;
import org.example.simu.playlist.security.CurrentUserResolver;
import org.example.simu.playlist.service.PlaylistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
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
 * Library of the authenticated user.
 * <p>
 * Ownership: playlist-service owns both the playlists and the library links, so the "my library"
 * collection is served here instead of being proxied through user-service. The caller id comes from
 * the identity header the gateway adds after validating the JWT, never from the request payload.
 */
@Tag(name = "My playlists", description = "Library of the authenticated user")
@RestController
@RequestMapping("/api/v1/me/playlists")
@Validated
@SecurityRequirement(name = "bearerAuth")
public class MyPlaylistsController {

    private static final Logger log = LoggerFactory.getLogger(MyPlaylistsController.class);

    private final PlaylistService playlistService;
    private final CurrentUserResolver currentUserResolver;

    public MyPlaylistsController(PlaylistService playlistService, CurrentUserResolver currentUserResolver) {
        this.playlistService = playlistService;
        this.currentUserResolver = currentUserResolver;
    }

    @Operation(summary = "List the playlists of the authenticated user")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PlaylistResponse>> getMyPlaylists(Authentication authentication,
                                                               HttpServletRequest request) {
        Long userId = currentUserResolver.resolveUserId(request);
        log.debug("Listing playlists of user id={}", userId);
        return ResponseEntity.ok(playlistService.getPlaylistsForUser(userId));
    }

    @Operation(summary = "Create a playlist for the authenticated user")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlaylistResponse> createMyPlaylist(Authentication authentication,
                                                             @Valid @RequestBody CreatePlaylistRequest body,
                                                             HttpServletRequest request,
                                                             UriComponentsBuilder uriBuilder) {
        Long userId = currentUserResolver.resolveUserId(request);
        log.debug("Creating playlist for user id={}", userId);

        PlaylistResponse response = playlistService.createPlaylistForUser(userId, body.name(), body.description());
        URI location = uriBuilder.path("/api/v1/playlists/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @ExceptionHandler(NotAuthenticatedException.class)
    public ResponseEntity<ErrorResponse> handleNotAuthenticated(NotAuthenticatedException ex) {
        log.warn("Unauthenticated request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ex.getMessage(), "NOT_AUTHENTICATED"));
    }
}