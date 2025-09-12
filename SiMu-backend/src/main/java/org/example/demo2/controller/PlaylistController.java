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
import jakarta.validation.constraints.Min;
import org.example.demo2.dto.CreatePlaylistRequest;
import org.example.demo2.dto.ErrorResponse;
import org.example.demo2.dto.PlaylistResponse;
import org.example.demo2.entity.PlayList;
import org.example.demo2.exception.PlaylistNotFoundException;
import org.example.demo2.exception.TrackNotFoundException;
import org.example.demo2.mapper.PlaylistMapper;
import org.example.demo2.services.PlaylistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * REST controller for managing playlists.
 * Provides endpoints for creating, updating, and retrieving playlists.
 */
@Tag(name = "Playlists", description = "API for managing playlists")
@RestController
@RequestMapping("/api/v1/playlists")
@Validated
@SecurityRequirement(name = "bearerAuth")
public class PlaylistController {

    private static final Logger logger = LoggerFactory.getLogger(PlaylistController.class);

    private final PlaylistService playlistService;
    private final PlaylistMapper playlistMapper;

    @Autowired
    public PlaylistController(PlaylistService playlistService, PlaylistMapper playlistMapper) {
        this.playlistService = playlistService;
        this.playlistMapper = playlistMapper;
    }

    /**
     * Creates a new playlist.
     *
     * @param request    The playlist creation request.
     * @param uriBuilder URI builder for location header.
     * @return Created playlist information.
     */
    @Operation(summary = "Create a new playlist", description = "Creates a new playlist for the authenticated user")
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
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlaylistResponse> createPlaylist(
            @Parameter(description = "Playlist creation data", required = true)
            @Valid @RequestBody CreatePlaylistRequest request,
            UriComponentsBuilder uriBuilder) {
        logger.info("Creating playlist with name: {}", request.name());
        PlayList playlist = playlistMapper.toEntity(request);
        PlaylistResponse response = playlistService.createPlayList(playlist);
        URI location = uriBuilder.path("/api/v1/playlists/{id}")
                .buildAndExpand(response.id())
                .toUri();
        logger.info("Playlist created successfully with ID: {}", response.id());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Adds a track to an existing playlist.
     *
     * @param playlistId The ID of the playlist.
     * @param musicId    The ID of the track to add.
     * @return Updated playlist information.
     */
    @Operation(summary = "Add track to playlist", description = "Adds a track to an existing playlist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Track added successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PlaylistResponse.class))),
            @ApiResponse(responseCode = "404", description = "Playlist or track not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/{playlistId}/tracks/{musicId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlaylistResponse> addTrackToPlaylist(
            @Parameter(description = "ID of the playlist", required = true)
            @PathVariable @Min(1) Long playlistId,
            @Parameter(description = "ID of the track to add", required = true)
            @PathVariable @Min(1) Long musicId) {
        logger.info("Adding track ID: {} to playlist ID: {}", musicId, playlistId);
        PlaylistResponse response = playlistService.addTrackToPlayList(playlistId, musicId);
        logger.info("Track ID: {} added to playlist ID: {}", musicId, playlistId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a playlist by ID.
     *
     * @param id The ID of the playlist.
     * @return Playlist information.
     */
    @Operation(summary = "Get playlist by ID", description = "Retrieves a playlist by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Playlist retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PlaylistResponse.class))),
            @ApiResponse(responseCode = "404", description = "Playlist not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlaylistResponse> getPlaylist(
            @Parameter(description = "ID of the playlist", required = true)
            @PathVariable @Min(1) Long id) {
        logger.info("Fetching playlist with ID: {}", id);
        PlaylistResponse playlist = playlistService.getPlaylist(id)
                .orElseThrow(() -> new PlaylistNotFoundException("Playlist not found with ID: " + id));
        logger.debug("Retrieved playlist: {}", playlist);
        return ResponseEntity.ok(playlist);
    }

    /**
     * Handles PlaylistNotFoundException.
     */
    @ExceptionHandler(PlaylistNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePlaylistNotFound(PlaylistNotFoundException ex) {
        logger.error("Playlist error: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), "PLAYLIST_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles TrackNotFoundException.
     */
    @ExceptionHandler(TrackNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTrackNotFound(TrackNotFoundException ex) {
        logger.error("Track error: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), "TRACK_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}