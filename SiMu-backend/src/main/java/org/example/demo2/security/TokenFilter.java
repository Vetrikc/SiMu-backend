package org.example.demo2.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filter for validating JWT tokens, setting authentication context, and verifying roles.
 */
@Component
public class TokenFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(TokenFilter.class);

    @Autowired
    private JwtCore jwtCore;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String jwt = null;
        String username = null;
        List<String> tokenRoles = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            if (jwt.isEmpty()) {
                logger.warn("Empty JWT token in Authorization header for request: {}", request.getRequestURI());
                sendError(response, HttpStatus.BAD_REQUEST, "TOKEN_EMPTY", "Token is empty");
                return;
            }

            try {

                username = jwtCore.getNameFromJwt(jwt);

                tokenRoles = jwtCore.getRolesFromJwt(jwt);
                logger.debug("Extracted username: {}, roles: {} from JWT", username, tokenRoles);
            } catch (ExpiredJwtException e) {
                logger.warn("JWT token expired for request: {}, error: {}", request.getRequestURI(), e.getMessage());
                sendError(response, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Token expired");
                return;
            } catch (JwtException e) {
                logger.warn("Invalid JWT token for request: {}, error: {}", request.getRequestURI(), e.getMessage());
                sendError(response, HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "Invalid token");
                return;
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                // Проверка соответствия ролей
                Set<String> userRoles = userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(authority -> authority.replace("ROLE_", ""))
                        .collect(Collectors.toSet());

                if (!userRoles.containsAll(tokenRoles)) {
                    logger.warn("Role mismatch for user: {}, token roles: {}, user roles: {}", username, tokenRoles, userRoles);
                    sendError(response, HttpStatus.FORBIDDEN, "ROLE_MISMATCH", "Token roles do not match user roles");
                    return;
                }

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                logger.debug("Authentication set for user: {} with roles: {}", username, userRoles);
            }
        } else {
            logger.debug("No valid Authorization header found for request: {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        String body = String.format("{\"message\": \"%s\", \"errorCode\": \"%s\"}", message, code);
        response.getWriter().write(body);
    }

}