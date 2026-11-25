package org.example.simu.playlist.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.simu.playlist.entity.IdempotencyKey;
import org.example.simu.playlist.repository.IdempotencyKeyRepository;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Replay guard for POSTs. The key is scoped by user, so two clients cannot collide, and only 2xx
 * responses are stored: a failed attempt must stay retryable.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String HEADER = "Idempotency-Key";

    private final IdempotencyKeyRepository repository;

    public IdempotencyFilter(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String key = request.getHeader(HEADER);
        return !"POST".equalsIgnoreCase(request.getMethod()) || key == null || key.isBlank();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Object attribute = request.getAttribute(HeaderAuthenticationFilter.USER_ID_ATTRIBUTE);
        long userId = attribute instanceof Long id ? id : 0L;
        String key = userId + ":" + request.getHeader(HEADER).trim();

        Optional<IdempotencyKey> stored = repository.findById(key);
        if (stored.isPresent()) {
            IdempotencyKey replay = stored.get();
            response.setStatus(replay.getResponseStatus());
            response.setHeader("Idempotent-Replay", "true");
            if (replay.getResponseBody() != null) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(replay.getResponseBody());
            }
            return;
        }

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrapper);

        int status = wrapper.getStatus();
        if (status >= 200 && status < 300) {
            String body = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            repository.save(new IdempotencyKey(key, userId, status, body));
        }
        wrapper.copyBodyToResponse();
    }
}
