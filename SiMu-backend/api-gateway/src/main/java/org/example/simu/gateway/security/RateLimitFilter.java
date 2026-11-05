package org.example.simu.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Fixed-window per-minute limiter for the auth endpoints, backed by Redis so several gateway
 * instances share one counter. Protects sign-in from password brute force and the mail endpoints
 * from being used as a spam relay.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String PREFIX = "rl:";

    private final ReactiveStringRedisTemplate redis;
    private final int signinPerMinute;
    private final int signupPerMinute;
    private final int recoveryPerMinute;
    private final int refreshPerMinute;

    public RateLimitFilter(ReactiveStringRedisTemplate redis,
                           @Value("${gateway.rate-limit.signin-per-minute:5}") int signinPerMinute,
                           @Value("${gateway.rate-limit.signup-per-minute:5}") int signupPerMinute,
                           @Value("${gateway.rate-limit.recovery-per-minute:3}") int recoveryPerMinute,
                           @Value("${gateway.rate-limit.refresh-per-minute:30}") int refreshPerMinute) {
        this.redis = redis;
        this.signinPerMinute = signinPerMinute;
        this.signupPerMinute = signupPerMinute;
        this.recoveryPerMinute = recoveryPerMinute;
        this.refreshPerMinute = refreshPerMinute;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        int limit = limitFor(path);
        if (limit <= 0) {
            return chain.filter(exchange);
        }

        String key = PREFIX + path + ":" + clientIp(exchange) + ":" + (Instant.now().getEpochSecond() / 60);

        return redis.opsForValue().increment(key)
                .flatMap(count -> count == 1
                        ? redis.expire(key, Duration.ofMinutes(2)).thenReturn(count)
                        : Mono.just(count))
                .flatMap(count -> {
                    if (count > limit) {
                        log.warn("Rate limit {} exceeded for {} on {}", limit, clientIp(exchange), path);
                        return tooManyRequests(exchange, limit);
                    }
                    return chain.filter(exchange);
                });
    }

    private int limitFor(String path) {
        return switch (path) {
            case "/api/v1/auth/signin" -> signinPerMinute;
            case "/api/v1/auth/signup" -> signupPerMinute;
            case "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                 "/api/v1/auth/verify-email" -> recoveryPerMinute;
            case "/api/v1/auth/refresh", "/api/v1/auth/logout" -> refreshPerMinute;
            default -> 0;
        };
    }

    private String clientIp(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() == null
                ? "unknown"
                : request.getRemoteAddress().getAddress().getHostAddress();
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange, int limit) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("Retry-After", "60");
        String body = "{\"message\": \"Too many requests, limit is " + limit
                + " per minute\", \"errorCode\": \"RATE_LIMIT_EXCEEDED\"}";
        var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
