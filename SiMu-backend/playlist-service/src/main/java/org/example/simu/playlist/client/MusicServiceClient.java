package org.example.simu.playlist.client;

import org.example.simu.playlist.dto.MusicDTO;
import org.example.simu.playlist.exception.MusicNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Validates a track against {@code music-service} before it is added to a playlist.
 */
@Component
public class MusicServiceClient {

    private final RestClient restClient;

    public MusicServiceClient(@Value("${services.music-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public MusicDTO getMusic(Long musicId) {
        try {
            MusicDTO music = restClient.get()
                    .uri("/api/v1/internal/music/{id}", musicId)
                    .retrieve()
                    .body(MusicDTO.class);
            if (music == null) {
                throw new MusicNotFoundException("Music not found with id: " + musicId);
            }
            return music;
        } catch (HttpClientErrorException.NotFound e) {
            throw new MusicNotFoundException("Music not found with id: " + musicId);
        }
    }
}
