package org.example.simu.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Issues access tokens.
 * <p>
 * Claim layout (understood by the gateway and by every service):
 * <ul>
 *     <li>{@code sub}   - username</li>
 *     <li>{@code roles} - list of roles without the {@code ROLE_} prefix</li>
 *     <li>{@code userId}- id of the user as a string</li>
 * </ul>
 */
@Component
public class JwtCore {

    private static final Logger log = LoggerFactory.getLogger(JwtCore.class);

    private final SecretKey signingKey;
    private final long tokenExpirationMs;

    public JwtCore(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms}") long tokenExpirationMs) {
        if (tokenExpirationMs <= 0) {
            throw new IllegalArgumentException("Token expiration time must be positive");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.tokenExpirationMs = tokenExpirationMs;
    }

    public String generateToken(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replace("ROLE_", ""))
                .toList();

        log.debug("Generating JWT for user: {}, roles: {}", userDetails.getUsername(), roles);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .claim("userId", String.valueOf(userDetails.getId()))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + tokenExpirationMs))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
}
