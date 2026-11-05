package org.example.simu.playlist.controller;

import jakarta.validation.Valid;
import org.example.simu.playlist.dto.PlaylistRequest;
import org.example.simu.playlist.dto.PlaylistResponse;
import org.example.simu.playlist.service.PlaylistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Service-to-service API. Called by {@code user-service} (playlists of a user) and by
 * {@code auth-service} (default playlist on signup). Not routed by the gateway.
 */
@RestController
@RequestMapping("/api/v1/internal/users/{userId}")
public class InternalPlaylistController {

    private final PlaylistService playlistService;

    public InternalPlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @GetMapping(value = "/playlists", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PlaylistResponse>> getPlaylists(@PathVariable Long userId) {
        return ResponseEntity.ok(playlistService.getPlaylistsForUser(userId));
    }

    @PostMapping(value = "/playlists", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlaylistResponse> createPlaylist(@PathVariable Long userId,
                                                           @Valid @RequestBody PlaylistRequest request) {
        PlaylistResponse response = playlistService.createPlaylistForUser(userId, request.name(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/default-playlist", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlaylistResponse> createDefaultPlaylist(@PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(playlistService.createDefaultPlaylistForUser(userId));
    }
}
