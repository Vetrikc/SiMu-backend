package org.example.simu.playlist.client;

import org.example.simu.playlist.dto.MusicDTO;
import org.example.simu.playlist.security.InternalApiKeyFilter;
import org.example.simu.playlist.exception.MusicNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Validates a track against {@code music-service} before it is added to a playlist.
 */
@Component
public class MusicServiceClient {

    private final RestClient restClient;

    public MusicServiceClient(@Value("${services.music-service.url}") String baseUrl,
                              @Value("${services.internal-api-key:}") String internalApiKey) {
        this.restClient = RestClient.builder().requestFactory(requestFactory())
                .baseUrl(baseUrl)
                .defaultHeader(InternalApiKeyFilter.HEADER, internalApiKey)
                .build();
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

    /**
     * Internal calls must fail fast: a hanging neighbour would otherwise pin request threads.
     */
    private static JdkClientHttpRequestFactory requestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(300))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(1));
        return factory;
    }
}
