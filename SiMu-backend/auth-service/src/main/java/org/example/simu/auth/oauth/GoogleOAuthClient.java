package org.example.simu.auth.oauth;

import org.example.simu.auth.config.GoogleOAuthProperties;
import org.example.simu.auth.exception.InvalidGoogleTokenException;
import org.example.simu.auth.exception.OAuthNotConfiguredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Talks to Google: builds the authorization URL and exchanges the authorization code for tokens.
 * The code exchange happens server-side only, the client secret never leaves the backend and PKCE
 * protects the code even if it leaks from the browser.
 */
@Component
public class GoogleOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthClient.class);

    private final GoogleOAuthProperties properties;
    private final RestClient restClient = RestClient.create();

    public GoogleOAuthClient(GoogleOAuthProperties properties) {
        this.properties = properties;
    }

    public String authorizationUrl(String state, String nonce, String codeChallenge) {
        requireConfigured();
        return UriComponentsBuilder.fromUriString(properties.authorizationUri())
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", properties.scope())
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                // values like "openid email profile" and the redirect URI must be percent-encoded
                .build()
                .encode()
                .toUriString();
    }

    public GoogleTokenResponse exchangeCode(String code, String codeVerifier) {
        requireConfigured();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", properties.redirectUri());
        form.add("code_verifier", codeVerifier);

        try {
            GoogleTokenResponse response = restClient.post()
                    .uri(properties.tokenUri())
                    // confidential client: the secret goes into the Basic header, not the body
                    .headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GoogleTokenResponse.class);

            if (response == null || response.idToken() == null) {
                throw new InvalidGoogleTokenException("Token endpoint did not return an ID token");
            }
            return response;
        } catch (HttpClientErrorException e) {
            // never log the code or the verifier
            log.warn("Google token endpoint rejected the code exchange: {}", e.getStatusCode());
            throw new InvalidGoogleTokenException("Authorization code was rejected by Google");
        }
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new OAuthNotConfiguredException("Google login is not configured on this environment");
        }
    }
}