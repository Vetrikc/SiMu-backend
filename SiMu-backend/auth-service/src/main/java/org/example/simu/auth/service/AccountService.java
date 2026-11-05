package org.example.simu.auth.service;

import org.example.simu.auth.entity.EmailToken;
import org.example.simu.auth.entity.User;
import org.example.simu.auth.exception.InvalidEmailTokenException;
import org.example.simu.auth.repository.EmailTokenRepository;
import org.example.simu.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * E-mail verification and password reset.
 * <p>
 * Both flows use single-use tokens (SHA-256 hashed in the database). Requesting a reset for an
 * unknown address still returns success - the API must not reveal which e-mails are registered.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long VERIFY_TTL_MS = 24 * 60 * 60 * 1000L;
    private static final long RESET_TTL_MS = 30 * 60 * 1000L;

    private final UserRepository userRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserDirectory userDirectory;

    public AccountService(UserRepository userRepository,
                          EmailTokenRepository emailTokenRepository,
                          EmailService emailService,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService,
                          UserDirectory userDirectory) {
        this.userRepository = userRepository;
        this.emailTokenRepository = emailTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.userDirectory = userDirectory;
    }

    @Transactional
    public void requestEmailVerification(User user) {
        if (user.isEmailVerified()) {
            return;
        }
        String raw = issueToken(user.getId(), EmailToken.Type.VERIFY_EMAIL, VERIFY_TTL_MS);
        emailService.sendVerification(user.getEmail(), raw);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        EmailToken token = consume(rawToken, EmailToken.Type.VERIFY_EMAIL);
        User user = loadUser(token.getUserId());
        user.setEmailVerified(true);
        userRepository.save(user);
        userDirectory.evict(user.getUsername());
        log.info("E-mail verified for user {}", user.getUsername());
    }

    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> user = userRepository.findByEmailIgnoreCase(email);
        if (user.isEmpty()) {
            log.info("Password reset requested for unknown address {}", email);
            return;
        }
        String raw = issueToken(user.get().getId(), EmailToken.Type.RESET_PASSWORD, RESET_TTL_MS);
        emailService.sendPasswordReset(user.get().getEmail(), raw);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        EmailToken token = consume(rawToken, EmailToken.Type.RESET_PASSWORD);
        User user = loadUser(token.getUserId());

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        // a password change invalidates every existing session
        tokenService.revokeAllForUser(user.getId());
        userDirectory.evict(user.getUsername());
        log.info("Password reset for user {}", user.getUsername());
    }

    private String issueToken(Long userId, EmailToken.Type type, long ttlMs) {
        String raw = newRawToken();
        EmailToken token = new EmailToken();
        token.setUserId(userId);
        token.setTokenHash(TokenService.hash(raw));
        token.setType(type);
        token.setExpiresAt(Instant.now().plusMillis(ttlMs));
        emailTokenRepository.save(token);
        return raw;
    }

    private EmailToken consume(String rawToken, EmailToken.Type expectedType) {
        EmailToken token = emailTokenRepository.findByTokenHash(TokenService.hash(rawToken))
                .orElseThrow(() -> new InvalidEmailTokenException("Token is unknown"));
        if (token.getType() != expectedType) {
            throw new InvalidEmailTokenException("Token has an unexpected type");
        }
        if (!token.isUsable()) {
            throw new InvalidEmailTokenException("Token is expired or already used");
        }
        token.setUsedAt(Instant.now());
        emailTokenRepository.save(token);
        return token;
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InvalidEmailTokenException("User of this token no longer exists"));
    }

    private String newRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
