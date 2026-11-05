package org.example.simu.gateway.config;

import org.example.simu.gateway.security.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declarative routing table of the gateway.
 * <p>
 * Every route first strips the internal {@code X-User-*} headers (so a client cannot spoof another
 * user) and then runs {@link AuthenticationFilter}, which validates the JWT and writes the verified
 * identity back into those headers.
 */
@Configuration
public class GatewayConfig {

    private final String authUrl;
    private final String userUrl;
    private final String musicUrl;
    private final String playlistUrl;

    public GatewayConfig(@Value("${auth-service.url}") String authUrl,
                         @Value("${user-service.url}") String userUrl,
                         @Value("${music-service.url}") String musicUrl,
                         @Value("${playlist-service.url}") String playlistUrl) {
        this.authUrl = authUrl;
        this.userUrl = userUrl;
        this.musicUrl = musicUrl;
        this.playlistUrl = playlistUrl;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, AuthenticationFilter authenticationFilter) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/v1/auth/**")
                        .filters(f -> secure(f, authenticationFilter))
                        .uri(authUrl))
                .route("user-service", r -> r.path("/api/v1/users/**")
                        .filters(f -> secure(f, authenticationFilter))
                        .uri(userUrl))
                .route("playlist-service", r -> r.path("/api/v1/playlists/**")
                        .filters(f -> secure(f, authenticationFilter))
                        .uri(playlistUrl))
                .route("music-service", r -> r.path("/api/v1/music/**")
                        .filters(f -> secure(f, authenticationFilter))
                        .uri(musicUrl))
                .build();
    }

    private GatewayFilterSpec secure(GatewayFilterSpec filters, AuthenticationFilter authenticationFilter) {
        return filters
                .removeRequestHeader("X-User-Id")
                .removeRequestHeader("X-User-Name")
                .removeRequestHeader("X-User-Roles")
                .filter(authenticationFilter);
    }
}
