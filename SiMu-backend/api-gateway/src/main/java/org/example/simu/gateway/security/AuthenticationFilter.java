package org.example.simu.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * JWT check for protected routes.
 * <p>
 * Besides the signature and the expiry, the token must not be in the Redis revocation lists
 * (logout of one token, deletion or role change of a user). On success the trusted identity is
 * forwarded to the downstream service inside {@code X-User-Id}, {@code X-User-Name} and
 * {@code X-User-Roles}; the services never parse the token themselves.
 */
@Component
public class AuthenticationFilter implements GatewayFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_NAME_HEADER = "X-User-Name";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final RouterValidator routerValidator;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklist;

    public AuthenticationFilter(RouterValidator routerValidator, JwtUtil jwtUtil, TokenBlacklistService blacklist) {
        this.routerValidator = routerValidator;
        this.jwtUtil = jwtUtil;
        this.blacklist = blacklist;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (!routerValidator.isSecured.test(request)) {
            return chain.filter(exchange);
        }

        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            return onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
        }

        String authHeader = authHeaders.get(0);
        if (!authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        if (token.isEmpty()) {
            return onError(exchange, "Token is empty", HttpStatus.BAD_REQUEST);
        }
        if (jwtUtil.isInvalid(token)) {
            return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }

        String jti = jwtUtil.extractJti(token);
        Long userId = jwtUtil.extractUserId(token);
        Long issuedAt = jwtUtil.extractIssuedAt(token);

        return blacklist.isRevoked(jti, userId, issuedAt)
                .flatMap(revoked -> {
                    if (Boolean.TRUE.equals(revoked)) {
                        return onError(exchange, "Token has been revoked", HttpStatus.UNAUTHORIZED);
                    }
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header(USER_NAME_HEADER, jwtUtil.extractSubject(token))
                            .header(USER_ROLES_HEADER, jwtUtil.extractRoles(token))
                            .headers(headers -> {
                                if (userId != null) {
                                    headers.set(USER_ID_HEADER, String.valueOf(userId));
                                }
                            })
                            .build();
                    log.debug("Forwarding {} {} as user {}", request.getMethod(), request.getPath(),
                            jwtUtil.extractSubject(token));
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                });
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        log.warn("Rejected request {} {}: {}", exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(), err);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }
}