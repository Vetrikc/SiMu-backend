package org.example.simu.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Reads the claims of the access token issued by {@code auth-service}.
 * The gateway and the auth service share one HS256 secret, so no call to the auth service is needed
 * to check a signature.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Tokens issued before the split into microservices do not carry the claim; callers must be
     * ready to receive {@code null}.
     */
    public Long extractUserId(String token) {
        Object raw = extractClaims(token).get("userId");
        return raw == null ? null : Long.valueOf(String.valueOf(raw));
    }

    @SuppressWarnings("unchecked")
    public String extractRoles(String token) {
        Object raw = extractClaims(token).get("roles");
        if (raw instanceof java.util.List<?> roles) {
            return roles.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        }
        return raw == null ? "" : String.valueOf(raw);
    }

    public boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    public boolean isInvalid(String token) {
        try {
            return isTokenExpired(token);
        } catch (Exception e) {
            return true;
        }
    }
}
