package org.example.demo2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.demo2.dto.ErrorResponse;
import org.example.demo2.dto.PlaylistRequest;
import org.example.demo2.dto.PlaylistResponse;
import org.example.demo2.dto.UserInfoResponse;
import org.example.demo2.exception.PlaylistNotFoundException;
import org.example.demo2.exception.UserNotFoundException;
import org.example.demo2.mapper.PlaylistMapper;
import org.example.demo2.entity.PlayList;
import org.example.demo2.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing user-related operations.
 * Provides endpoints for retrieving user info and managing playlists.
 */
@Tag(name = "Users", description = "API for managing user information and playlists")
@RestController
@RequestMapping("/api/v1/users")
@Validated
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final PlaylistMapper playlistMapper;

    @Autowired
    public UserController(UserService userService, PlaylistMapper playlistMapper) {
        this.userService = userService;
        this.playlistMapper = playlistMapper;
    }

    /**
     * Retrieves information about the authenticated user.
     *
     * @param authentication The Spring Security authentication object.
     * @return User information.
     */
    @Operation(summary = "Get user information", description = "Returns information about the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User info retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserInfoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserInfoResponse> getUserInfo(
            @Parameter(hidden = true) Authentication authentication) {
        logger.info("Fetching user info for: {}", authentication.getName());
        String username = authentication.getName();
        UserInfoResponse response = userService.getUserInfo(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        logger.debug("Retrieved user info: {}", response);
        return ResponseEntity.ok(response);
    }

    /**
     * Adds a new playlist for the authenticated user.
     *
     * @param authentication The Spring Security authentication object.
     * @param playlistRequest The playlist data to create.
     * @param uriBuilder URI builder for location header.
     * @return Created playlist information.
     */
    @Operation(summary = "Add a new playlist", description = "Creates a new playlist for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Playlist created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PlaylistResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid playlist data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/playlists", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PlaylistResponse> addPlaylist(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "Playlist data to create", required = true)
            @Valid @RequestBody PlaylistRequest playlistRequest,
            UriComponentsBuilder uriBuilder) {
        String username = authentication.getName();
        logger.info("Adding playlist for user: {}", username);
        PlayList playlist = playlistMapper.toEntity(playlistRequest);
        PlayList savedPlaylist = userService.addPlaylistToUser(username, playlist);
        PlaylistResponse response = playlistMapper.toResponse(savedPlaylist);
        URI location = uriBuilder.path("/api/v1/playlists/{id}")
                .buildAndExpand(savedPlaylist.getId())
                .toUri();
        logger.info("Playlist created successfully for user: {}, playlist ID: {}", username, savedPlaylist.getId());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Retrieves all playlists for the authenticated user.
     *
     * @param authentication The Spring Security authentication object.
     * @return List of user playlists.
     */
    @Operation(summary = "Get user playlists", description = "Returns a list of playlists for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Playlists retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PlaylistResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(value = "/playlists", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PlaylistResponse>> getUserPlaylists(
            @Parameter(hidden = true) Authentication authentication) {
        String username = authentication.getName();
        logger.info("Fetching playlists for user: {}", username);
        List<PlayList> playlists = userService.getUserPlaylists(username);
        List<PlaylistResponse> response = playlists.stream()
                .map(playlistMapper::toResponse)
                .collect(Collectors.toList());
        logger.debug("Retrieved {} playlists for user: {}", response.size(), username);
        return ResponseEntity.ok(response);
    }
    @GetMapping(value = "/playlists1", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("response");
    }

    /**
     * Handles UserNotFoundException.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        logger.error("User not found: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), "USER_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles PlaylistNotFoundException.
     */
    @ExceptionHandler(PlaylistNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePlaylistNotFound(PlaylistNotFoundException ex) {
        logger.error("Playlist not found: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), "PLAYLIST_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}