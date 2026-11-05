package org.example.simu.playlist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.simu.playlist.dto.CreatePlaylistRequest;
import org.example.simu.playlist.dto.ErrorResponse;
import org.example.simu.playlist.dto.PlaylistResponse;
import org.example.simu.playlist.exception.MusicNotFoundException;
import org.example.simu.playlist.exception.PlaylistNotFoundException;
import org.example.simu.playlist.exception.TrackNotFoundException;
import org.example.simu.playlist.service.PlaylistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Public contract for {@code /api/v1/playlists}; unchanged compared to the monolith.
 */
@Tag(name = "Playlists", description = "API for managing playlists")
@RestController
@RequestMapping("/api/v1/playlists")
@Validated
@SecurityRequirement(name = "bearerAuth")
public class PlaylistController {

    private static final Logger log = LoggerFactory.getLogger(PlaylistController.class);

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @Operation(summary = "Create a new playlist", description = "Creates a new playlist")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlaylistResponse> createPlaylist(@Valid @RequestBody CreatePlaylistRequest request,
                                                           UriComponentsBuilder uriBuilder) {
        log.info("Creating playlist with name: {}", request.name());
        PlaylistResponse response = playlistService.createPlaylist(request.name(), request.description());
        URI location = uriBuilder.path("/api/v1/playlists/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Add track to playlist", description = "Adds a track to an existing playlist")
    @PostMapping(value = "/{playlistId}/tracks/{musicId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlaylistResponse> addTrackToPlaylist(@PathVariable Long playlistId,
                                                               @PathVariable Long musicId) {
        log.info("Adding track ID: {} to playlist ID: {}", musicId, playlistId);
        return ResponseEntity.ok(playlistService.addTrackToPlaylist(playlistId, musicId));
    }

    @Operation(summary = "Get playlist by ID", description = "Retrieves a playlist by its ID")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlaylistResponse> getPlaylist(@PathVariable Long id) {
        log.info("Fetching playlist with ID: {}", id);
        PlaylistResponse playlist = playlistService.getPlaylist(id)
                .orElseThrow(() -> new PlaylistNotFoundException("Playlist not found with ID: " + id));
        return ResponseEntity.ok(playlist);
    }

    @ExceptionHandler(PlaylistNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePlaylistNotFound(PlaylistNotFoundException ex) {
        log.warn("Playlist error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), "PLAYLIST_NOT_FOUND"));
    }

    @ExceptionHandler(TrackNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTrackNotFound(TrackNotFoundException ex) {
        log.warn("Track error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), "TRACK_NOT_FOUND"));
    }

    @ExceptionHandler(MusicNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMusicNotFound(MusicNotFoundException ex) {
        log.warn("Music error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), "MUSIC_NOT_FOUND"));
    }
}
