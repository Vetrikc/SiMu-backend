package org.example.simu.gateway.security;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Lists the paths that may be called without a token.
 * Everything else has to carry a valid {@code Authorization: Bearer ...} header.
 */
@Component
public class RouterValidator {

    private static final List<Pattern> OPEN_PATHS = List.of(
            Pattern.compile("^/api/v1/auth/signin$"),
            Pattern.compile("^/api/v1/auth/signup$"),
            Pattern.compile("^/actuator/.*")
    );

    public final Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getPath().value();
        return OPEN_PATHS.stream().noneMatch(pattern -> pattern.matcher(path).matches());
    };
}
