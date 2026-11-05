package org.example.simu.user.security;

import jakarta.servlet.http.HttpServletRequest;
import org.example.simu.user.client.AuthServiceClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Resolves the numeric id of the caller.
 * The gateway already put it into {@code X-User-Id}; if the service is called directly (Swagger,
 * curl with a token) the id is read from the token claim or, as a last resort, from auth-service.
 */
@Component
public class CurrentUserResolver {

    private final AuthServiceClient authServiceClient;

    public CurrentUserResolver(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    public Long resolveUserId(Authentication authentication, HttpServletRequest request) {
        Object attribute = request.getAttribute(HeaderAuthenticationFilter.USER_ID_ATTRIBUTE);
        if (attribute instanceof Long id) {
            return id;
        }

        String header = request.getHeader(HeaderAuthenticationFilter.USER_ID_HEADER);
        if (header != null && !header.isBlank()) {
            try {
                return Long.valueOf(header);
            } catch (NumberFormatException ignored) {
                // fall through to auth-service
            }
        }

        return authServiceClient.getUser(authentication.getName()).id();
    }
}
