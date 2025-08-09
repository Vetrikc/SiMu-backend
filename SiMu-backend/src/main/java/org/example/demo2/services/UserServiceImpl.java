package org.example.demo2.services;

import jakarta.validation.Valid;
import org.example.demo2.entity.PlayList;
import org.example.demo2.entity.User;
import org.example.demo2.repository.PlayListRepository;
import org.example.demo2.repository.UserRepository;
import org.example.demo2.security.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Service for managing user-related operations.
 */
@Service
@Validated
public class UserServiceImpl implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayListRepository playListRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User '" + username + "' not found"));
        return UserDetailsImpl.build(user);
    }

    @Transactional
    public User addNowCreatedPlaylistToUser(String username, @Valid PlayList playlist) {
        Objects.requireNonNull(username, "Username must not be null");
        Objects.requireNonNull(playlist, "Playlist must not be null");

        logger.debug("Adding playlist to user: {}", username);
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User with username " + username + " not found"));

        PlayList savedPlaylist = playListRepository.save(playlist);
        user.addPlaylist(savedPlaylist);
        return userRepository.save(user);
    }

    public List<PlayList> getUserPlaylists(String username) {
        Objects.requireNonNull(username, "Username must not be null");
        logger.debug("Fetching playlists for user: {}", username);
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User with username " + username + " not found"));
        return user.getPlaylists();
    }

    @Transactional
    public User createUser(String username, String email, String password) {
        Objects.requireNonNull(username, "Username must not be null");
        Objects.requireNonNull(email, "Email must not be null");
        Objects.requireNonNull(password, "Password must not be null");

        logger.debug("Creating user: {}", username);
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRoles(Set.of("USER")); // Назначаем роль по умолчанию

        PlayList favorite = new PlayList();
        favorite.setName("Favorites");
        favorite.setDescription("Default playlist for user " + username);
        favorite.setImageUrl("default_image_url");
        PlayList savedPlaylist = playListRepository.save(favorite);
        user.addPlaylist(savedPlaylist);

        User savedUser = userRepository.save(user);
        logger.info("User created successfully: {}", username);
        return savedUser;
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}