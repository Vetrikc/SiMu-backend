package org.example.simu.auth.oauth;

/**
 * The only thing we take from Google: a stable subject and a verified e-mail.
 */
public record GoogleIdentity(String subject, String email, boolean emailVerified, String name) {
}
