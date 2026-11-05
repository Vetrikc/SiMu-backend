package org.example.simu.auth.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Keeps the pending authorization request in Redis for 10 minutes.
 * <p>
 * The key is a random flow id that the browser receives in an HttpOnly cookie, so the flow is bound
 * to the browser that started it. {@link #consume(String)} deletes the entry before any validation
 * happens (Redis {@code GETDEL}), which makes a replayed callback impossible even under a race.
 */
@Component
public class OAuthFlowStore {

    private static final Logger log = LoggerFactory.getLogger(OAuthFlowStore.class);
    private static final String PREFIX = "oauth:flow:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public OAuthFlowStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public String save(OAuthFlow flow) {
        String flowId = Pkce.newRandomValue();
        try {
            redis.opsForValue().set(PREFIX + flowId, objectMapper.writeValueAsString(flow), TTL);
        } catch (Exception e) {
            throw new IllegalStateException("Could not store the OAuth flow", e);
        }
        return flowId;
    }

    /**
     * Reads and deletes the flow in one atomic step. A second callback for the same browser gets
     * an empty result.
     */
    public Optional<OAuthFlow> consume(String flowId) {
        if (flowId == null || flowId.isBlank()) {
            return Optional.empty();
        }
        String payload = redis.opsForValue().getAndDelete(PREFIX + flowId);
        if (payload == null) {
            log.warn("OAuth callback without a pending flow (replay or expired)");
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, OAuthFlow.class));
        } catch (Exception e) {
            throw new IllegalStateException("Corrupted OAuth flow payload", e);
        }
    }
}
