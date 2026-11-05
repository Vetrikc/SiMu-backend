package org.example.simu.auth.service;

import org.example.simu.auth.config.RedisConfig;
import org.example.simu.auth.dto.UserDetailsResponse;
import org.example.simu.auth.exception.UserNotFoundException;
import org.example.simu.auth.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of the identity directory, cached in Redis.
 * Kept separate from {@link UserService} so the cache proxy never wraps a write transaction.
 */
@Service
public class UserDirectory {

    private final UserRepository userRepository;

    public UserDirectory(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Cacheable(cacheNames = RedisConfig.USER_CACHE, key = "#username")
    @Transactional(readOnly = true)
    public UserDetailsResponse getUserDetails(String username) {
        return userRepository.findUserByUsername(username)
                .map(user -> new UserDetailsResponse(user.getId(), user.getUsername(), user.getEmail(),
                        user.getRoles(), user.isEmailVerified()))
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }

    @CacheEvict(cacheNames = RedisConfig.USER_CACHE, key = "#username")
    public void evict(String username) {
        // eviction is declared by the annotation
    }
}
