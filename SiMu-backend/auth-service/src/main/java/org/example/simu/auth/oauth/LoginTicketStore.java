package org.example.simu.auth.oauth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * The callback runs as a top-level browser navigation, so tokens cannot be returned in its body
 * without leaking them into history or the address bar. Instead the callback redirects to the
 * frontend with a one-time ticket (60 s, single use) that the SPA exchanges for the token pair.
 */
@Component
public class LoginTicketStore {

    private static final String PREFIX = "oauth:ticket:";
    private static final Duration TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;

    public LoginTicketStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String issue(Long userId) {
        String ticket = Pkce.newRandomValue();
        redis.opsForValue().set(PREFIX + ticket, String.valueOf(userId), TTL);
        return ticket;
    }

    public Optional<Long> consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return Optional.empty();
        }
        String value = redis.opsForValue().getAndDelete(PREFIX + ticket);
        return value == null ? Optional.empty() : Optional.of(Long.valueOf(value));
    }
}
