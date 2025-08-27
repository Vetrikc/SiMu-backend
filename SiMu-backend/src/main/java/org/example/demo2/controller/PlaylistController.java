package org.example.demo2.controller;

import jakarta.validation.Valid;
import org.example.demo2.dto.CreatePlaylistRequest;
import org.example.demo2.entity.PlayList;
import org.example.demo2.services.PlayListService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller for managing playlists.
 */
@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {
    private static final Logger logger = LoggerFactory.getLogger(PlaylistController.class);

    @Autowired
    private PlayListService playlistService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlayList> createPlaylist(@Valid @RequestBody CreatePlaylistRequest request) {
        logger.debug("Creating playlist with name: {}", request.getName());
        PlayList createdPlaylist = playlistService.createPlayList(request.getName(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPlaylist);
    }

    @PostMapping("/{playlistId}/tracks/{musicId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlayList> addTrackToPlaylist(@PathVariable Long playlistId, @PathVariable Long musicId) {
        logger.debug("Adding musicId={} to playlistId={}", musicId, playlistId);
        PlayList updatedPlaylist = playlistService.addTrackToPlayList(playlistId, musicId);
        return ResponseEntity.ok(updatedPlaylist);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlayList> getPlaylist(@PathVariable Long id) {
        logger.debug("Fetching playlist with id: {}", id);
        Optional<PlayList> playlist = playlistService.getPlaylist(id);
        return playlist.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}