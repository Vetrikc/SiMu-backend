package org.example.simu.auth.oauth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PkceTest {

    /**
     * Test vector from RFC 7636, Appendix B.
     */
    @Test
    void challengeMatchesRfcVector() {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        assertThat(Pkce.challenge(verifier)).isEqualTo("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
    }

    @Test
    void generatedVerifierHasRfcLengthAndIsUrlSafe() {
        String verifier = Pkce.newVerifier();
        assertThat(verifier).hasSize(43);
        assertThat(verifier).matches("^[A-Za-z0-9_-]+$");
    }

    @Test
    void verifierAndChallengeAreDifferentEachTime() {
        String first = Pkce.newVerifier();
        String second = Pkce.newVerifier();
        assertThat(first).isNotEqualTo(second);
        assertThat(Pkce.challenge(first)).isNotEqualTo(Pkce.challenge(second));
    }
}
