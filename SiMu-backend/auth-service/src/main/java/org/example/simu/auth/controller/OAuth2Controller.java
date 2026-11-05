package org.example.simu.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.example.simu.auth.config.GoogleOAuthProperties;
import org.example.simu.auth.dto.ExchangeTicketRequest;
import org.example.simu.auth.dto.SigninResponse;
import org.example.simu.auth.entity.User;
import org.example.simu.auth.oauth.ExternalLoginService;
import org.example.simu.auth.oauth.GoogleIdentity;
import org.example.simu.auth.oauth.GoogleIdentityVerifier;
import org.example.simu.auth.oauth.GoogleOAuthClient;
import org.example.simu.auth.oauth.GoogleTokenResponse;
import org.example.simu.auth.oauth.LoginTicketStore;
import org.example.simu.auth.oauth.OAuthFlow;
import org.example.simu.auth.oauth.OAuthFlowStore;
import org.example.simu.auth.oauth.Pkce;
import org.example.simu.auth.exception.InvalidOAuthStateException;
import org.example.simu.auth.exception.InvalidTicketException;
import org.example.simu.auth.service.LoginAuditService;
import org.example.simu.auth.service.TokenPair;
import org.example.simu.auth.service.TokenService;
import org.example.simu.auth.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

/**
 * Google login: Authorization Code Flow with PKCE.
 * <ol>
 *     <li>{@code GET /google} stores state/nonce/verifier server-side (10 min, browser-bound cookie)
 *         and redirects to Google;</li>
 *     <li>{@code GET /google/callback} consumes the pending flow <em>before</em> validating it,
 *         compares the state in constant time, exchanges the code server-side, verifies the ID token,
 *         links the identity and redirects to the frontend with a one-time ticket;</li>
 *     <li>{@code POST /exchange} turns that ticket into the project's own access/refresh pair.</li>
 * </ol>
 */
@Tag(name = "OAuth2", description = "Sign in with Google")
@RestController
@RequestMapping("/api/v1/auth/oauth2")
public class OAuth2Controller {

    private static final String FLOW_COOKIE = "oauth_flow";
    private static final String FLOW_COOKIE_PATH = "/api/v1/auth/oauth2";
    private static final long FLOW_TTL_MS = 10 * 60 * 1000L;

    private final GoogleOAuthClient googleOAuthClient;
    private final GoogleIdentityVerifier identityVerifier;
    private final OAuthFlowStore flowStore;
    private final LoginTicketStore ticketStore;
    private final ExternalLoginService externalLoginService;
    private final UserService userService;
    private final TokenService tokenService;
    private final LoginAuditService loginAuditService;
    private final GoogleOAuthProperties properties;

    public OAuth2Controller(GoogleOAuthClient googleOAuthClient,
                            GoogleIdentityVerifier identityVerifier,
                            OAuthFlowStore flowStore,
                            LoginTicketStore ticketStore,
                            ExternalLoginService externalLoginService,
                            UserService userService,
                            TokenService tokenService,
                            LoginAuditService loginAuditService,
                            GoogleOAuthProperties properties) {
        this.googleOAuthClient = googleOAuthClient;
        this.identityVerifier = identityVerifier;
        this.flowStore = flowStore;
        this.ticketStore = ticketStore;
        this.externalLoginService = externalLoginService;
        this.userService = userService;
        this.tokenService = tokenService;
        this.loginAuditService = loginAuditService;
        this.properties = properties;
    }

    @Operation(summary = "Start Google login")
    @GetMapping("/google")
    public ResponseEntity<Void> start(HttpServletResponse response) {
        String state = Pkce.newRandomValue();
        String nonce = Pkce.newRandomValue();
        String verifier = Pkce.newVerifier();

        OAuthFlow flow = new OAuthFlow(state, nonce, verifier, properties.redirectUri(),
                System.currentTimeMillis());
        String flowId = flowStore.save(flow);

        response.addHeader(HttpHeaders.SET_COOKIE, flowCookie(flowId).toString());
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(googleOAuthClient.authorizationUrl(state, nonce, Pkce.challenge(verifier))))
                .build();
    }

    @Operation(summary = "Google redirect target")
    @GetMapping("/google/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         @CookieValue(name = FLOW_COOKIE, required = false) String flowId,
                                         HttpServletRequest httpRequest,
                                         HttpServletResponse httpResponse) {
        // consume before validating: a replayed or raced callback finds nothing
        OAuthFlow flow = flowStore.consume(flowId)
                .orElseThrow(() -> new InvalidOAuthStateException("There is no pending Google login"));
        expireFlowCookie(httpResponse);

        if (!constantTimeEquals(state, flow.state())) {
            throw new InvalidOAuthStateException("State parameter does not match");
        }
        if (System.currentTimeMillis() - flow.createdAt() > FLOW_TTL_MS) {
            throw new InvalidOAuthStateException("Google login took too long");
        }
        if (code == null || code.isBlank()) {
            throw new InvalidOAuthStateException("Authorization code is missing");
        }

        GoogleTokenResponse tokens = googleOAuthClient.exchangeCode(code, flow.verifier());
        GoogleIdentity identity = identityVerifier.verify(tokens.idToken(), flow.nonce());
        User user = linkIdentity(identity);

        loginAuditService.record(user.getId(), user.getUsername(), true, "GOOGLE", httpRequest,
                "GOOGLE", TokenService.hash(identity.subject()));

        String ticket = ticketStore.issue(user.getId());
        String redirect = properties.frontendRedirectUri() + "?ticket=" + ticket;
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirect)).build();
    }

    @Operation(summary = "Exchange the one-time ticket for a token pair")
    @PostMapping("/exchange")
    public ResponseEntity<SigninResponse> exchange(@Valid @RequestBody ExchangeTicketRequest request,
                                                   HttpServletRequest httpRequest) {
        Long userId = ticketStore.consume(request.ticket())
                .orElseThrow(() -> new InvalidTicketException("Login ticket is unknown or already used"));
        User user = userService.findById(userId);

        TokenPair pair = tokenService.issue(user, clientIp(httpRequest), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(new SigninResponse(pair.accessToken(), pair.refreshToken(),
                pair.expiresIn(), pair.refreshExpiresIn()));
    }

    /**
     * Two parallel callbacks with the same Google subject can race on the unique key
     * {@code (provider, subject)}; the loser simply re-reads the row created by the winner.
     */
    private User linkIdentity(GoogleIdentity identity) {
        try {
            return externalLoginService.findOrCreate(identity);
        } catch (DataIntegrityViolationException e) {
            return externalLoginService.findOrCreate(identity);
        }
    }

    private ResponseCookie flowCookie(String flowId) {
        return ResponseCookie.from(FLOW_COOKIE, flowId)
                .httpOnly(true)
                .secure(properties.secureCookie())
                .sameSite("Lax")
                .path(FLOW_COOKIE_PATH)
                .maxAge(Duration.ofMinutes(10))
                .build();
    }

    private void expireFlowCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(FLOW_COOKIE, "")
                .httpOnly(true).secure(properties.secureCookie()).sameSite("Lax")
                .path(FLOW_COOKIE_PATH).maxAge(0).build().toString());
    }

    private boolean constantTimeEquals(String provided, String stored) {
        if (provided == null || stored == null || provided.length() != stored.length()) {
            return false;
        }
        return MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8),
                stored.getBytes(StandardCharsets.UTF_8));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}