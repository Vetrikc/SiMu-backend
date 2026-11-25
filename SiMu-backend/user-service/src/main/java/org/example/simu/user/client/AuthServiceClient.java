package org.example.simu.user.client;

import org.example.simu.user.dto.UserDetailsResponse;
import org.example.simu.user.security.InternalApiKeyFilter;
import org.example.simu.user.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Reads and deletes accounts in {@code auth-service} - the only service that owns the
 * {@code users} table.
 */
@Component
public class AuthServiceClient {

    private final RestClient restClient;

    public AuthServiceClient(@Value("${services.auth-service.url}") String baseUrl,
                             @Value("${services.internal-api-key:}") String internalApiKey) {
        this.restClient = RestClient.builder().requestFactory(requestFactory())
                .baseUrl(baseUrl)
                .defaultHeader(InternalApiKeyFilter.HEADER, internalApiKey)
                .build();
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
