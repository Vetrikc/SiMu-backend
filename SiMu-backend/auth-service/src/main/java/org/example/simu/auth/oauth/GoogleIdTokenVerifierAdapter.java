package org.example.simu.auth.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.example.simu.auth.config.GoogleOAuthProperties;
import org.example.simu.auth.exception.InvalidGoogleTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Verifies Google ID tokens with the official library (signature against Google JWKS, audience,
 * expiry, issuer) and additionally checks the nonce, which the library does not compare for us.
 */
@Component
public class GoogleIdTokenVerifierAdapter implements GoogleIdentityVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleIdTokenVerifierAdapter.class);
    private static final String CURRENT_ISSUER = "https://accounts.google.com";
    private static final String LEGACY_ISSUER = "accounts.google.com";

    private final GoogleOAuthProperties properties;
    private final GoogleIdTokenVerifier currentIssuerVerifier;
    private final GoogleIdTokenVerifier legacyIssuerVerifier;

    public GoogleIdTokenVerifierAdapter(GoogleOAuthProperties properties) {
        this.properties = properties;
        NetHttpTransport transport = new NetHttpTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        this.currentIssuerVerifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(List.of(properties.clientId()))
                .setIssuer(CURRENT_ISSUER)
                .build();
        this.legacyIssuerVerifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(List.of(properties.clientId()))
                .setIssuer(LEGACY_ISSUER)
                .build();
    }

    @Override
    public GoogleIdentity verify(String rawIdToken, String expectedNonce) {
        if (properties.clientId() == null || properties.clientId().isBlank()) {
            throw new InvalidGoogleTokenException("Google login is not configured");
        }
        GoogleIdToken idToken = parse(rawIdToken);
        GoogleIdToken.Payload payload = idToken.getPayload();

        // the library checks signature/audience/expiry/issuer; the nonce is ours to check
        String nonceFromToken = (String) payload.get("nonce");
        if (expectedNonce == null || !Objects.equals(expectedNonce, nonceFromToken)) {
            log.warn("Google ID token has an unexpected nonce");
            throw new InvalidGoogleTokenException("Google ID token has an unexpected nonce");
        }

        return new GoogleIdentity(
                payload.getSubject(),
                payload.getEmail(),
                Boolean.TRUE.equals(payload.getEmailVerified()),
                (String) payload.get("name"));
    }

    private GoogleIdToken parse(String rawIdToken) {
        try {
            GoogleIdToken token = currentIssuerVerifier.verify(rawIdToken);
            if (token == null) {
                token = legacyIssuerVerifier.verify(rawIdToken);
            }
            if (token == null) {
                throw new InvalidGoogleTokenException("Google ID token is invalid");
            }
            return token;
        } catch (InvalidGoogleTokenException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not verify the Google ID token: {}", e.getMessage());
            throw new InvalidGoogleTokenException("Google ID token is invalid");
        }
    }
}
