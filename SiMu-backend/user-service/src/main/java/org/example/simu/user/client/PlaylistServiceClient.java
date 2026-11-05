package org.example.simu.user.client;

import org.example.simu.user.dto.PlaylistRequest;
import org.example.simu.user.security.InternalApiKeyFilter;
import org.example.simu.user.dto.PlaylistResponse;
import org.example.simu.user.exception.PlaylistNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

/**
 * Playlist operations of a concrete user. {@code playlist-service} owns the playlists themselves,
 * this service only exposes them under the {@code /api/v1/users} part of the public contract.
 */
@Component
public class PlaylistServiceClient {

    private final RestClient restClient;

    public PlaylistServiceClient(@Value("${services.playlist-service.url}") String baseUrl,
                                 @Value("${services.internal-api-key:}") String internalApiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(InternalApiKeyFilter.HEADER, internalApiKey)
                .build();
    }

    public List<PlaylistResponse> getUserPlaylists(Long userId) {
        PlaylistResponse[] response = restClient.get()
                .uri("/api/v1/internal/users/{userId}/playlists", userId)
                .retrieve()
                .body(PlaylistResponse[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }

    public PlaylistResponse createPlaylist(Long userId, PlaylistRequest request) {
        try {
            PlaylistResponse response = restClient.post()
                    .uri("/api/v1/internal/users/{userId}/playlists", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(PlaylistResponse.class);
            if (response == null) {
                throw new PlaylistNotFoundException("Playlist service returned an empty response");
            }
            return response;
        } catch (HttpClientErrorException.NotFound e) {
            throw new PlaylistNotFoundException("Playlist could not be created for user id: " + userId);
        }
    }
}