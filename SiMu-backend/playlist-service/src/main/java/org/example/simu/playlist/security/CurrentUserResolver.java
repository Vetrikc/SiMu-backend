package org.example.simu.playlist.security;

import jakarta.servlet.http.HttpServletRequest;
import org.example.simu.playlist.exception.NotAuthenticatedException;
import org.springframework.stereotype.Component;

/**
 * The only way this service learns who the caller is: the id that the gateway put into
 * {@code X-User-Id} (or, for direct calls, the {@code userId} claim of a verified token).
 * A user id coming from the query string or the body is never read.
 */
@Component
public class CurrentUserResolver {

    public Long resolveUserId(HttpServletRequest request) {
        Object attribute = request.getAttribute(HeaderAuthenticationFilter.USER_ID_ATTRIBUTE);
        if (attribute instanceof Long userId) {
            return userId;
        }
        throw new NotAuthenticatedException("User id is missing from the request");
    }
}
