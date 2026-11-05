package org.example.simu.auth.client;

import org.example.simu.auth.security.InternalApiKeyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls {@code playlist-service} to provision the default "Favorites" playlist of a new user.
 * <p>
 * The monolith created that playlist inside the signup transaction. Now it is a second service, so
 * the call is explicitly non-fatal: a temporary outage of the playlist service must not block
 * registration.
 */
@Component
public class PlaylistClient {

    private static final Logger log = LoggerFactory.getLogger(PlaylistClient.class);

    private final RestClient restClient;

    public PlaylistClient(@Value("${services.playlist-service.url}") String baseUrl,
                          @Value("${services.internal-api-key:}") String internalApiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(InternalApiKeyFilter.HEADER, internalApiKey)
                .build();
    }

    public void createDefaultPlaylist(Long userId) {
        try {
            restClient.post()
                    .uri("/api/v1/internal/users/{userId}/default-playlist", userId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.warn("Could not create the default playlist for user id={}: {}", userId, e.getMessage());
        }
    }
}