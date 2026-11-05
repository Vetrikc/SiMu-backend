package org.example.simu.user.client;

import org.example.simu.user.dto.UserDetailsResponse;
import org.example.simu.user.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Reads and deletes accounts in {@code auth-service} - the only service that owns the
 * {@code users} table.
 */
@Component
public class AuthServiceClient {

    private final RestClient restClient;

    public AuthServiceClient(@Value("${services.auth-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public UserDetailsResponse getUser(String username) {
        try {
            UserDetailsResponse response = restClient.get()
                    .uri("/api/v1/internal/users/{username}", username)
                    .retrieve()
                    .body(UserDetailsResponse.class);
            if (response == null) {
                throw new UserNotFoundException("User not found: " + username);
            }
            return response;
        } catch (HttpClientErrorException.NotFound e) {
            throw new UserNotFoundException("User not found: " + username);
        }
    }

    public void deleteUser(Long userId) {
        try {
            restClient.delete()
                    .uri("/api/v1/internal/users/{id}", userId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new UserNotFoundException("User not found: " + userId);
        }
    }
}
