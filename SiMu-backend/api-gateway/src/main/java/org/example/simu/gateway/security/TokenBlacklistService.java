package org.example.simu.gateway.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks the revocation lists written by auth-service.
 * <p>
 * All three keys are fetched with a single {@code MGET} instead of three round trips, and a
 * positive "not revoked" result is cached for one second in memory. The cache is deliberately
 * one-sided: a revoked token is never cached as valid, so revocation takes effect within a second
 * without hitting Redis on every request.
 */
@Component
public class TokenBlacklistService {

    private static final String JTI_PREFIX = "auth:revoked:";
    private static final String USER_PREFIX = "auth:user-blocked:";
    private static final String INVALID_BEFORE_PREFIX = "auth:user-invalid-before:";

    private final ReactiveStringRedisTemplate redis;
    private final Cache<String, Boolean> notRevokedCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(1))
            .maximumSize(200_000)
            .build();

    public TokenBlacklistService(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    public Mono<Boolean> isRevoked(String jti, Long userId, Long issuedAt) {
        String cacheKey = jti + "|" + userId + "|" + issuedAt;
        if (Boolean.TRUE.equals(notRevokedCache.getIfPresent(cacheKey))) {
            return Mono.just(false);
        }

        List<String> keys = new ArrayList<>(3);
        if (jti != null && !jti.isBlank()) {
            keys.add(JTI_PREFIX + jti);
        }
        if (userId != null) {
            keys.add(USER_PREFIX + userId);
        }
        if (userId != null && issuedAt != null) {
            keys.add(INVALID_BEFORE_PREFIX + userId);
        }
        if (keys.isEmpty()) {
            return Mono.just(false);
        }

        return redis.opsForValue().multiGet(keys)
                .map(values -> revoked(values, issuedAt))
                .doOnNext(revoked -> {
                    if (!revoked) {
                        notRevokedCache.put(cacheKey, true);
                    }
                })
                .defaultIfEmpty(false);
    }

    private boolean revoked(List<String> values, Long issuedAt) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (String value : values) {
            if (value == null) {
                continue;
            }
            if (issuedAt == null) {
                return true;
            }
            try {
                if (issuedAt <= Long.parseLong(value)) {
                    return true;
                }
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return false;
    }
}
