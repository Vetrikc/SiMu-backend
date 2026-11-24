package org.example.simu.playlist.controller;

import org.example.simu.playlist.dto.PlaylistResponse;
import org.example.simu.playlist.service.PlaylistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service API. Called by {@code auth-service} on signup to create the default playlist.
 * The library itself is public through {@code /api/v1/me/playlists}, so no proxying is needed.
 * Not routed by the gateway.
 */
@RestController
@RequestMapping("/api/v1/internal/users/{userId}")
public class InternalPlaylistController {

    private final PlaylistService playlistService;

    public InternalPlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @PostMapping(value = "/default-playlist", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlaylistResponse> createDefaultPlaylist(@PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(playlistService.createDefaultPlaylistForUser(userId));
    }
}