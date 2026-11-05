package org.example.simu.auth.oauth;

/**
 * Server-side state of one pending authorization request.
 */
public record OAuthFlow(String state, String nonce, String verifier, String redirectUri, long createdAt) {
}
