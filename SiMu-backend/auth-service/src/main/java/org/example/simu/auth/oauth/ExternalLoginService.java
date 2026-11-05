package org.example.simu.auth.oauth;

import org.example.simu.auth.entity.User;
import org.example.simu.auth.entity.UserIdentity;
import org.example.simu.auth.exception.EmailNotVerifiedException;
import org.example.simu.auth.exception.InvalidGoogleTokenException;
import org.example.simu.auth.repository.UserIdentityRepository;
import org.example.simu.auth.repository.UserRepository;
import org.example.simu.auth.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Links a Google identity to a local account.
 * <p>
 * Rules that matter for security:
 * <ul>
 *     <li>the identity is matched by {@code (provider, subject)} - e-mail is not a stable key;</li>
 *     <li>an existing account is only linked when Google reports the e-mail as verified, otherwise
 *         an attacker could take over an account by registering the same address at Google;</li>
 *     <li>a brand-new account gets no password at all: password login stays impossible for it.</li>
 * </ul>
 */
@Service
public class ExternalLoginService {

    private static final Logger log = LoggerFactory.getLogger(ExternalLoginService.class);
    private static final String PROVIDER_GOOGLE = "google";

    private final UserIdentityRepository userIdentityRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public ExternalLoginService(UserIdentityRepository userIdentityRepository,
                                UserRepository userRepository,
                                UserService userService) {
        this.userIdentityRepository = userIdentityRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Transactional
    public User findOrCreate(GoogleIdentity identity) {
        if (identity.subject() == null || identity.subject().isBlank()) {
            throw new InvalidGoogleTokenException("Google ID token has no subject");
        }

        Optional<UserIdentity> existing =
                userIdentityRepository.findByProviderAndSubject(PROVIDER_GOOGLE, identity.subject());
        if (existing.isPresent()) {
            return userRepository.findById(existing.get().getUserId())
                    .orElseThrow(() -> new InvalidGoogleTokenException("Linked account no longer exists"));
        }

        if (!identity.emailVerified()) {
            // MUST NOT link: Google may hand out unverified addresses for custom domains
            throw new EmailNotVerifiedException("Google did not verify this e-mail address");
        }
        if (identity.email() == null || identity.email().isBlank()) {
            throw new InvalidGoogleTokenException("Google ID token has no e-mail");
        }

        User user = userRepository.findByEmailIgnoreCase(identity.email())
                .orElseGet(() -> userService.createExternalUser(uniqueUsername(identity), identity.email()));

        UserIdentity link = new UserIdentity();
        link.setUserId(user.getId());
        link.setProvider(PROVIDER_GOOGLE);
        link.setSubject(identity.subject());
        link.setEmail(identity.email());
        userIdentityRepository.save(link);

        log.info("Google identity {} linked to user id={}", identity.subject(), user.getId());
        return user;
    }

    private String uniqueUsername(GoogleIdentity identity) {
        String local = identity.email() == null ? "" : identity.email().split("@")[0];
        String candidate = local.replaceAll("[^A-Za-z0-9_-]", "");
        if (candidate.length() < 2) {
            candidate = "google" + identity.subject().replaceAll("[^A-Za-z0-9]", "");
        }
        if (candidate.length() > 16) {
            candidate = candidate.substring(0, 16);
        }
        String base = candidate;
        int suffix = 0;
        while (userRepository.existsByUsername(candidate)) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
    }
}
