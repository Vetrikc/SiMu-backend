package org.example.simu.auth.oauth;

public interface GoogleIdentityVerifier {

    /**
     * Validates the ID token (signature, issuer, audience, expiry, nonce) and returns the identity.
     *
     * @throws org.example.simu.auth.exception.InvalidGoogleTokenException when anything does not match
     */
    GoogleIdentity verify(String rawIdToken, String expectedNonce);
}
