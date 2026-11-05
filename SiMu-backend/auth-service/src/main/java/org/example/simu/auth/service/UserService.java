package org.example.simu.auth.service;

import org.example.simu.auth.entity.User;
import org.example.simu.auth.exception.UserNotFoundException;
import org.example.simu.auth.repository.UserRepository;
import org.example.simu.auth.security.UserDetailsImpl;
import org.example.simu.common.event.UserDeletedEvent;
import org.example.simu.common.event.UserRegisteredEvent;
import org.example.simu.common.uuid.UuidV7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

/**
 * Write side of the identity directory. Every state change and its integration event are committed
 * in the same transaction through the outbox.
 */
@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final OutboxWriter outboxWriter;
    private final UserDirectory userDirectory;

    public UserService(UserRepository userRepository, OutboxWriter outboxWriter, UserDirectory userDirectory) {
        this.userRepository = userRepository;
        this.outboxWriter = outboxWriter;
        this.userDirectory = userDirectory;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User '" + username + "' not found"));
        return UserDetailsImpl.build(user);
    }

    @Transactional
    public User createUser(String username, String email, String encodedPassword) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setRoles(Set.of("USER"));

        User saved = userRepository.save(user);
        outboxWriter.append("User", saved.getId(), "UserRegistered",
                new UserRegisteredEvent(UuidV7.generate(), Instant.now(), saved.getId(), saved.getUsername(), saved.getEmail()));

        log.info("User created: id={}, username={}", saved.getId(), saved.getUsername());
        return saved;
    }

    /**
     * Soft delete: the row is kept for audit, the account disappears from every query and the other
     * services learn about it through Kafka.
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));

        user.markDeleted();
        userRepository.save(user);
        outboxWriter.append("User", id, "UserDeleted",
                new UserDeletedEvent(UuidV7.generate(), Instant.now(), id));
        userDirectory.evict(user.getUsername());

        log.info("User soft-deleted: id={}, username={}", id, user.getUsername());
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
