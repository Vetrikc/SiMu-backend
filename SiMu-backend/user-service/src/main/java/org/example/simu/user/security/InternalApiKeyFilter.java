package org.example.simu.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protects {@code /api/v1/internal/**}: only another service that knows the shared key may call it.
 * The gateway strips the header from every inbound request, so a client cannot fake it.
 * <p>
 * A constant-time comparison is used to avoid leaking the key through timing.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Token";
    private static final String INTERNAL_PREFIX = "/api/v1/internal/";
    private static final Logger log = LoggerFactory.getLogger(InternalApiKeyFilter.class);

    private final byte[] expectedKey;

    public InternalApiKeyFilter(@Value("${services.internal-api-key:}") String apiKey) {
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith(INTERNAL_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(HEADER);
        if (expectedKey.length == 0 || provided == null
                || !MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), expectedKey)) {
            log.warn("Rejected internal call to {} from {}", path, request.getRemoteAddr());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\": \"Internal API key is missing or invalid\", \"errorCode\": \"INTERNAL_UNAUTHORIZED\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}