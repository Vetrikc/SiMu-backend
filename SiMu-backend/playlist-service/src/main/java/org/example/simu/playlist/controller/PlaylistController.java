package org.example.simu.playlist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.simu.playlist.dto.ErrorResponse;
import org.example.simu.playlist.dto.PlaylistResponse;
import org.example.simu.playlist.exception.MusicNotFoundException;
import org.example.simu.playlist.exception.PlaylistNotFoundException;
import org.example.simu.playlist.exception.TrackNotFoundException;
import org.example.simu.playlist.security.CurrentUserResolver;
import org.example.simu.playlist.service.PlaylistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public contract for {@code /api/v1/playlists}.
 * <p>
 * Every operation is scoped to the caller: a playlist is reachable only through a live library
 * link of the authenticated user, so a foreign id answers {@code 404}.
 */
@Tag(name = "Playlists", description = "API for managing playlists")
@RestController
@RequestMapping("/api/v1/playlists")
@Validated
@SecurityRequirement(name = "bearerAuth")
public class PlaylistController {

    private static final Logger log = LoggerFactory.getLogger(PlaylistController.class);

    private final PlaylistService playlistService;
    private final CurrentUserResolver currentUserResolver;

    public PlaylistController(PlaylistService playlistService, CurrentUserResolver currentUserResolver) {
        this.playlistService = playlistService;
        this.currentUserResolver = currentUserResolver;
    }

    @Operation(summary = "Add track to playlist", description = "Adds a track to an existing playlist")
    @PostMapping(value = "/{playlistId}/tracks/{musicId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlaylistResponse> addTrackToPlaylist(@PathVariable Long playlistId,
                                                               @PathVariable Long musicId,
                                                               HttpServletRequest request) {
        Long userId = currentUserResolver.resolveUserId(request);
        log.info("Adding track ID: {} to playlist ID: {} of user {}", musicId, playlistId, userId);
        return ResponseEntity.ok(playlistService.addTrackToPlaylist(playlistId, musicId, userId));
    }

    @Operation(summary = "Get playlist by ID", description = "Retrieves a playlist by its ID")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlaylistResponse> getPlaylist(@PathVariable Long id, HttpServletRequest request) {
        Long userId = currentUserResolver.resolveUserId(request);
        log.info("Fetching playlist with ID: {} of user {}", id, userId);
        PlaylistResponse playlist = playlistService.getPlaylist(id, userId)
                .orElseThrow(() -> new PlaylistNotFoundException("Playlist not found with ID: " + id));
        return ResponseEntity.ok(playlist);
    }

    @Operation(summary = "Delete a playlist", description = "Soft-deletes the playlist and its library links")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deletePlaylist(@PathVariable Long id, HttpServletRequest request) {
        Long userId = currentUserResolver.resolveUserId(request);
        log.info("Deleting playlist ID: {} of user {}", id, userId);
        playlistService.deletePlaylist(id, userId);
        return ResponseEntity.noContent().build();
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