package org.example.simu.auth.oauth;

/**
 * PKCE helpers (RFC 7636) and the random material of an OAuth flow.
 * Only the S256 challenge method is supported - plain must never be used.
 */
public final class Pkce {

    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
    private static final java.util.Base64.Encoder ENCODER =
            java.util.Base64.getUrlEncoder().withoutPadding();

    private Pkce() {
    }

    /** 32 random bytes -> 43 base64url characters, inside the 43-128 range required by the RFC. */
    public static String newVerifier() {
        return randomToken();
    }

    public static String newRandomValue() {
        return randomToken();
    }

    /** code_challenge = BASE64URL(SHA256(ASCII(code_verifier))). */
    public static String challenge(String verifier) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            return ENCODER.encodeToString(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
