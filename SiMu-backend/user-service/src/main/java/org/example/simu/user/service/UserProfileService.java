package org.example.simu.user.service;

import org.example.simu.common.event.UserRegisteredEvent;
import org.example.simu.common.uuid.UuidV7;
import org.example.simu.user.client.AuthServiceClient;
import org.example.simu.user.config.RedisConfig;
import org.example.simu.user.dto.UserDetailsResponse;
import org.example.simu.user.dto.UserInfoResponse;
import org.example.simu.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Read model first: the profile normally comes from the local table filled by Kafka.
 * If the projection has not caught up yet (fresh signup, replayed topic), it falls back to the
 * auth-service and stores the result, so the read path is self-healing.
 */
@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository userProfileRepository;
    private final AuthServiceClient authServiceClient;
    private final ProfileProjectionService projectionService;
    private final CacheManager cacheManager;

    public UserProfileService(UserProfileRepository userProfileRepository,
                              AuthServiceClient authServiceClient,
                              ProfileProjectionService projectionService,
                              CacheManager cacheManager) {
        this.userProfileRepository = userProfileRepository;
        this.authServiceClient = authServiceClient;
        this.projectionService = projectionService;
        this.cacheManager = cacheManager;
    }

    @Cacheable(cacheNames = RedisConfig.PROFILE_CACHE, key = "#username")
    public UserInfoResponse getProfile(String username) {
        return userProfileRepository.findByUsername(username)
                .map(profile -> new UserInfoResponse(profile.getUsername()))
                .orElseGet(() -> fallbackToAuthService(username));
    }

    public void deleteProfileByUserId(Long userId) {
        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.markDeleted();
            userProfileRepository.save(profile);
            evict(profile.getUsername());
            log.info("Profile soft-deleted for user id={}", userId);
        });
    }

    private UserInfoResponse fallbackToAuthService(String username) {
        UserDetailsResponse remote = authServiceClient.getUser(username);
        try {
            projectionService.applyRegistered(new UserRegisteredEvent(
                    UuidV7.generate(), Instant.now(), remote.id(), remote.username(), remote.email()));
        } catch (RuntimeException e) {
            log.warn("Could not persist the profile projection for {}: {}", username, e.getMessage());
        }
        return new UserInfoResponse(remote.username());
    }

    private void evict(String username) {
        Cache cache = cacheManager.getCache(RedisConfig.PROFILE_CACHE);
        if (cache != null) {
            cache.evict(username);
        }
    }
}
