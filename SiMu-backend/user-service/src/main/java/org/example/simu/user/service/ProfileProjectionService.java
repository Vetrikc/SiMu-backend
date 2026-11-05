package org.example.simu.user.service;

import org.example.simu.common.event.UserDeletedEvent;
import org.example.simu.common.event.UserRegisteredEvent;
import org.example.simu.user.config.RedisConfig;
import org.example.simu.user.entity.ProcessedEvent;
import org.example.simu.user.entity.UserProfile;
import org.example.simu.user.repository.ProcessedEventRepository;
import org.example.simu.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies user lifecycle events to the local read model. Events are deduplicated by
 * {@code event_id}, so a redelivered Kafka record is a no-op.
 */
@Service
public class ProfileProjectionService {

    private static final Logger log = LoggerFactory.getLogger(ProfileProjectionService.class);

    private final UserProfileRepository userProfileRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final CacheManager cacheManager;

    public ProfileProjectionService(UserProfileRepository userProfileRepository,
                                    ProcessedEventRepository processedEventRepository,
                                    CacheManager cacheManager) {
        this.userProfileRepository = userProfileRepository;
        this.processedEventRepository = processedEventRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public void applyRegistered(UserRegisteredEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.debug("Skipping duplicate event {}", event.eventId());
            return;
        }

        UserProfile profile = userProfileRepository.findByUserId(event.userId()).orElseGet(UserProfile::new);
        profile.setUserId(event.userId());
        profile.setUsername(event.username());
        profile.setEmail(event.email());
        userProfileRepository.save(profile);

        processedEventRepository.save(new ProcessedEvent(event.eventId(), "UserRegistered"));
        evict(event.username());
        log.info("Profile projected for user id={}, username={}", event.userId(), event.username());
    }

    @Transactional
    public void applyDeleted(UserDeletedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }

        userProfileRepository.findByUserId(event.userId()).ifPresent(profile -> {
            profile.markDeleted();
            userProfileRepository.save(profile);
            evict(profile.getUsername());
            log.info("Profile soft-deleted for user id={}", event.userId());
        });

        processedEventRepository.save(new ProcessedEvent(event.eventId(), "UserDeleted"));
    }

    private void evict(String username) {
        Cache cache = cacheManager.getCache(RedisConfig.PROFILE_CACHE);
        if (cache != null) {
            cache.evict(username);
        }
    }
}
