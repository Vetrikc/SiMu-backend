package org.example.simu.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.simu.common.uuid.UuidV7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Issues short-lived access tokens.
 * <p>
 * Claim layout (understood by the gateway and by every service):
 * <ul>
 *     <li>{@code sub}     - username</li>
 *     <li>{@code roles}   - list of roles without the {@code ROLE_} prefix</li>
 *     <li>{@code userId}  - id of the user as a string</li>
 *     <li>{@code jti}     - token id, used by the logout blacklist</li>
 *     <li>{@code typ}     - always {@code access}, so other token kinds are rejected</li>
 * </ul>
 */
@Component
public class JwtCore {

    private static final Logger log = LoggerFactory.getLogger(JwtCore.class);
    private static final String TYPE_ACCESS = "access";

    private final SecretKey signingKey;
    private final long accessExpirationMs;

    public JwtCore(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.access-expiration-ms:3600000}") long accessExpirationMs) {
        if (accessExpirationMs <= 0) {
            throw new IllegalArgumentException("Access token expiration must be positive");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    public String generateAccessToken(Long userId, String username, Collection<String> roles) {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(username)
                .claim("userId", String.valueOf(userId))
                .claim("roles", List.copyOf(roles))
                .claim("typ", TYPE_ACCESS)
                // millisecond precision: role changes invalidate everything issued before a mark,
                // and second-precision `iat` is too coarse to separate old and new tokens
                .claim("iatMs", now.toEpochMilli())
                .id(UuidV7.generate().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpirationMs)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        log.debug("Issued access token for user {} with roles {}", username, roles);
        return token;
    }

    public String generateAccessToken(Authentication authentication) {
        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replace("ROLE_", ""))
                .toList();
        return generateAccessToken(principal.getId(), principal.getUsername(), roles);
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}