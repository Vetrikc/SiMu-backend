package org.example.simu.music.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Rebuilds the security context from the headers set by the gateway ({@code X-User-*}) or, when a
 * service is called directly (Swagger, tests, local debugging), from the bearer token itself.
 * <p>
 * The filter never touches a database: identity is fully described by the signed token.
 */
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_NAME_HEADER = "X-User-Name";
    public static final String USER_ROLES_HEADER = "X-User-Roles";
    public static final String USER_ID_ATTRIBUTE = "simu.userId";

    private static final Logger log = LoggerFactory.getLogger(HeaderAuthenticationFilter.class);

    private final SecretKey signingKey;

    public HeaderAuthenticationFilter(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String username = request.getHeader(USER_NAME_HEADER);
            String rolesHeader = request.getHeader(USER_ROLES_HEADER);
            String userIdHeader = request.getHeader(USER_ID_HEADER);

            if (username == null) {
                String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (authorization != null && authorization.startsWith("Bearer ")) {
                    String token = authorization.substring(7);
                    if (token.isEmpty()) {
                        writeError(response, HttpStatus.BAD_REQUEST, "Token is empty", "TOKEN_EMPTY");
                        return;
                    }
                    try {
                        Claims claims = Jwts.parser()
                                .verifyWith(signingKey)
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();
                        username = claims.getSubject();
                        if (rolesHeader == null) {
                            rolesHeader = joinRoles(claims.get("roles"));
                        }
                        if (userIdHeader == null && claims.get("userId") != null) {
                            userIdHeader = String.valueOf(claims.get("userId"));
                        }
                    } catch (ExpiredJwtException e) {
                        log.warn("JWT token expired for request: {}", request.getRequestURI());
                        writeError(response, HttpStatus.UNAUTHORIZED, "Token expired", "TOKEN_EXPIRED");
                        return;
                    } catch (JwtException | IllegalArgumentException e) {
                        log.warn("Invalid JWT token for request: {}", request.getRequestURI());
                        writeError(response, HttpStatus.UNAUTHORIZED, "Invalid token", "TOKEN_INVALID");
                        return;
                    }
                }
            }

            if (username != null && !username.isBlank()) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, parseAuthorities(rolesHeader));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (userIdHeader != null && !userIdHeader.isBlank()) {
                    try {
                        request.setAttribute(USER_ID_ATTRIBUTE, Long.valueOf(userIdHeader));
                    } catch (NumberFormatException ignored) {
                        log.debug("Ignoring malformed {} header", USER_ID_HEADER);
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String joinRoles(Object raw) {
        if (raw instanceof List<?> roles) {
            return roles.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        }
        return raw == null ? "" : String.valueOf(raw);
    }

    private Collection<GrantedAuthority> parseAuthorities(String rolesHeader) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return authorities;
        }
        Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                .forEach(authorities::add);
        return authorities;
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message, String errorCode)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(String.format("{\"message\": \"%s\", \"errorCode\": \"%s\"}", message, errorCode));
    }
}