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

    public String extractSubject(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return extractClaims(token).getId();
    }

    /**
     * @return issuance time in epoch millis (the {@code iatMs} claim, falling back to {@code iat})
     */
    public Long extractIssuedAt(String token) {
        Claims claims = extractClaims(token);
        Object precise = claims.get("iatMs");
        if (precise != null) {
            return Long.valueOf(String.valueOf(precise));
        }
        Date issuedAt = claims.getIssuedAt();
        return issuedAt == null ? null : issuedAt.getTime();
    }

    public String extractTokenType(String token) {
        Object typ = extractClaims(token).get("typ");
        return typ == null ? "access" : String.valueOf(typ);
    }

    /**
     * Tokens issued before the split into microservices do not carry the claim; callers must be
     * ready to receive {@code null}.
     */
    public Long extractUserId(String token) {
        Object raw = extractClaims(token).get("userId");
        return raw == null ? null : Long.valueOf(String.valueOf(raw));
    }

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
            return isTokenExpired(token) || !"access".equals(extractTokenType(token));
        } catch (Exception e) {
            return true;
        }
    }
}