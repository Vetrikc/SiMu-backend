package org.example.simu.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google OAuth2 client settings. Empty {@code clientId} means "Google login is disabled".
 */
@ConfigurationProperties(prefix = "oauth2.google")
public record GoogleOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String authorizationUri,
        String tokenUri,
        String scope,
        String frontendRedirectUri,
        boolean secureCookie
) {

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && redirectUri != null && !redirectUri.isBlank();
    }
}
