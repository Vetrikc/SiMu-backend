package org.example.simu.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.simu.auth.dto.ErrorResponse;
import org.example.simu.auth.dto.LogoutRequest;
import org.example.simu.auth.dto.RefreshRequest;
import org.example.simu.auth.dto.SigninResponse;
import org.example.simu.auth.exception.InvalidRefreshTokenException;
import org.example.simu.auth.service.TokenPair;
import org.example.simu.auth.service.TokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Refresh with rotation and logout.
 * <p>
 * Reusing an already rotated refresh token revokes the whole token family - that is the signal of a
 * stolen token and forces a new login.
 */
@Tag(name = "Authentication", description = "Refresh and logout")
@RestController
@RequestMapping("/api/v1/auth")
public class TokenController {

    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Operation(summary = "Rotate the token pair")
    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SigninResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                                  HttpServletRequest httpRequest) {
        TokenPair pair = tokenService.rotate(request.refreshToken(),
                clientIp(httpRequest), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(new SigninResponse(pair.accessToken(), pair.refreshToken(),
                pair.expiresIn(), pair.refreshExpiresIn()));
    }

    @Operation(summary = "Logout: revoke the refresh token and blacklist the access token")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequest request,
                                       @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String accessToken = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : null;
        tokenService.logout(request == null ? null : request.refreshToken(), accessToken);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefresh(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ex.getMessage(), "INVALID_REFRESH_TOKEN"));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
