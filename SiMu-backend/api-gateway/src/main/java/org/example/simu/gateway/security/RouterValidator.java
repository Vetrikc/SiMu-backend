package org.example.simu.gateway.security;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Paths that may be called without an access token.
 */
@Component
public class RouterValidator {

    private static final List<Pattern> OPEN_PATHS = List.of(
            Pattern.compile("^/api/v1/auth/signin$"),
            Pattern.compile("^/api/v1/auth/signup$"),
            Pattern.compile("^/api/v1/auth/refresh$"),
            Pattern.compile("^/api/v1/auth/logout$"),
            Pattern.compile("^/api/v1/auth/forgot-password$"),
            Pattern.compile("^/api/v1/auth/reset-password$"),
            Pattern.compile("^/api/v1/auth/verify-email$"),
            Pattern.compile("^/actuator/.*")
    );

    public final Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getPath().value();
        return OPEN_PATHS.stream().noneMatch(pattern -> pattern.matcher(path).matches());
    };
}
