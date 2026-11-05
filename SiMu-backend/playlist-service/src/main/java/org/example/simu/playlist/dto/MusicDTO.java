package org.example.simu.playlist.dto;

/**
 * Read model of a track returned by {@code music-service}.
 */
public record MusicDTO(Long id, String name, String artist, String url) {
}
