package org.example.simu.gateway.security;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks the two revocation lists written by auth-service:
 * <ul>
 *     <li>{@code auth:revoked:{jti}} - a single access token revoked by logout;</li>
 *     <li>{@code auth:user-blocked:{userId}} - every token of a user, used after an account
 *         deletion or a role change.</li>
 * </ul>
 * Keys expire on their own, so the lists stay small.
 */
@Component
public class TokenBlacklistService {

    private static final String JTI_PREFIX = "auth:revoked:";
    private static final String USER_PREFIX = "auth:user-blocked:";
    private static final String INVALID_BEFORE_PREFIX = "auth:user-invalid-before:";

    private final ReactiveStringRedisTemplate redis;

    public TokenBlacklistService(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    public Mono<Boolean> isRevoked(String jti, Long userId, Long issuedAt) {
        List<Mono<Boolean>> checks = new ArrayList<>(3);
        if (jti != null && !jti.isBlank()) {
            checks.add(redis.hasKey(JTI_PREFIX + jti));
        }
        if (userId != null) {
            checks.add(redis.hasKey(USER_PREFIX + userId));
        }
        if (userId != null && issuedAt != null) {
            checks.add(redis.opsForValue().get(INVALID_BEFORE_PREFIX + userId)
                    .map(value -> {
                        try {
                            return issuedAt <= Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }));
        }
        if (checks.isEmpty()) {
            return Mono.just(false);
        }
        return Flux.merge(checks).any(Boolean::booleanValue);
    }
}