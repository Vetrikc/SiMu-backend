package org.example.simu.music.controller;

import org.example.simu.music.dto.ErrorResponse;
import org.example.simu.music.dto.MusicDTO;
import org.example.simu.music.exception.MusicNotFoundException;
import org.example.simu.music.service.MusicService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service API used by {@code playlist-service} to validate that a track exists before it
 * is added to a playlist.
 */
@RestController
@RequestMapping("/api/v1/internal/music")
public class InternalMusicController {

    private final MusicService musicService;

    public InternalMusicController(MusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MusicDTO> getMusic(@PathVariable Long id) {
        return ResponseEntity.ok(musicService.getMusic(id));
    }

    @ExceptionHandler(MusicNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMusicNotFound(MusicNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), "MUSIC_NOT_FOUND"));
    }
}
