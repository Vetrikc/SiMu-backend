package org.example.simu.auth.service;

import io.jsonwebtoken.Claims;
import org.example.simu.auth.entity.RefreshToken;
import org.example.simu.auth.entity.User;
import org.example.simu.auth.exception.InvalidRefreshTokenException;
import org.example.simu.auth.repository.RefreshTokenRepository;
import org.example.simu.auth.repository.UserRepository;
import org.example.simu.auth.security.JwtCore;
import org.example.simu.common.uuid.UuidV7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Issues, rotates and revokes token pairs.
 * <ul>
 *     <li>access token - JWT, short lived (default 1 hour), stateless;</li>
 *     <li>refresh token - opaque random string, stored only as a SHA-256 hash, rotated on every use;</li>
 *     <li>replay of an already rotated refresh token revokes the whole family (stolen token case);</li>
 *     <li>logout also puts the access token {@code jti} into a Redis blacklist with TTL equal to the
 *         remaining token lifetime, so the gateway rejects it immediately.</li>
 * </ul>
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    static final String BLACKLIST_PREFIX = "auth:revoked:";

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtCore jwtCore;
    private final StringRedisTemplate redis;
    private final long refreshExpirationMs;

    public TokenService(RefreshTokenRepository refreshTokenRepository,
                        UserRepository userRepository,
                        JwtCore jwtCore,
                        StringRedisTemplate redis,
                        @Value("${jwt.refresh-expiration-ms:2592000000}") long refreshExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtCore = jwtCore;
        this.redis = redis;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Transactional
    public TokenPair issue(User user, String ip, String userAgent) {
        String rawRefresh = newRawToken();
        refreshTokenRepository.save(buildRefreshToken(user.getId(), UuidV7.generate(), rawRefresh, ip, userAgent));
        String accessToken = jwtCore.generateAccessToken(user.getId(), user.getUsername(), user.getRoles());
        return new TokenPair(accessToken, rawRefresh, accessTtlSeconds(), refreshTtlSeconds());
    }

    /**
     * Rotates the pair: the presented token is revoked and replaced by a new one of the same family.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenPair rotate(String rawRefreshToken, String ip, String userAgent) {
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is unknown"));

        if (current.getRevokedAt() != null) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), Instant.now());
            log.warn("Refresh token reuse detected for user {}, family {} revoked",
                    current.getUserId(), current.getFamilyId());
            throw new InvalidRefreshTokenException("Refresh token has already been used");
        }
        if (current.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        User user = userRepository.findById(current.getUserId())
                .orElseThrow(() -> new InvalidRefreshTokenException("User of this token no longer exists"));

        current.setRevokedAt(Instant.now());

        String rawNext = newRawToken();
        RefreshToken next = refreshTokenRepository.save(
                buildRefreshToken(user.getId(), current.getFamilyId(), rawNext, ip, userAgent));
        current.setReplacedBy(next.getId());
        refreshTokenRepository.save(current);

        String accessToken = jwtCore.generateAccessToken(user.getId(), user.getUsername(), user.getRoles());
        return new TokenPair(accessToken, rawNext, accessTtlSeconds(), refreshTtlSeconds());
    }

    /**
     * Revokes the presented refresh token and blacklists the presented access token.
     */
    @Transactional
    public void logout(String rawRefreshToken, String accessToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                    .filter(token -> token.getRevokedAt() == null)
                    .ifPresent(token -> {
                        token.setRevokedAt(Instant.now());
                        refreshTokenRepository.save(token);
                    });
        }
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                Claims claims = jwtCore.parse(accessToken);
                blacklist(claims.getId(), claims.getExpiration().toInstant());
            } catch (RuntimeException e) {
                log.debug("Could not blacklist access token: {}", e.getMessage());
            }
        }
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        int revoked = refreshTokenRepository.revokeAllOfUser(userId, Instant.now());
        log.info("Revoked {} refresh tokens of user {}", revoked, userId);
    }

    /**
     * Blocks every access token of a user for the rest of its lifetime: the gateway checks
     * {@code auth:user-blocked:{userId}}. Used after an account deletion or a role change, where
     * the already issued claims are no longer trustworthy.
     */
    public void invalidateTokensIssuedBefore(Long userId, Instant instant) {
        redis.opsForValue().set("auth:user-invalid-before:" + userId,
                String.valueOf(instant.toEpochMilli()), Duration.ofMillis(jwtCore.getAccessExpirationMs()));
        log.info("Tokens of user {} issued before {} are invalidated", userId, instant);
    }

    public void blockUserTokens(Long userId) {
        redis.opsForValue().set("auth:user-blocked:" + userId, "1",
                Duration.ofMillis(jwtCore.getAccessExpirationMs()));
        log.info("Access tokens of user {} blocked", userId);
    }

    private void blacklist(String jti, Instant expiresAt) {
        if (jti == null) {
            return;
        }
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redis.opsForValue().set(BLACKLIST_PREFIX + jti, "1", ttl);
    }

    private RefreshToken buildRefreshToken(Long userId, UUID familyId, String raw, String ip, String userAgent) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(hash(raw));
        token.setFamilyId(familyId);
        token.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        token.setIp(ip);
        token.setUserAgent(truncate(userAgent, 255));
        return token;
    }

    private String newRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }

    private long accessTtlSeconds() {
        return jwtCore.getAccessExpirationMs() / 1000;
    }

    private long refreshTtlSeconds() {
        return refreshExpirationMs / 1000;
    }

    public static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}