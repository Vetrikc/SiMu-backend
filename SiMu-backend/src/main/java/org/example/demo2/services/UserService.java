package org.example.demo2.services;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.example.demo2.dto.PlaylistResponse;
import org.example.demo2.dto.UserInfoResponse;
import org.example.demo2.entity.PlayList;
import org.example.demo2.entity.User;
import org.example.demo2.exception.UserNotFoundException;
import org.example.demo2.mapper.PlaylistMapper;
import org.example.demo2.mapper.UserMapper;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service implementation for managing user-related operations.
 */
@Service
@Validated
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PlayListRepository playListRepository;
    private final UserMapper userMapper;
    private final PlaylistMapper playlistMapper;

    @Autowired
    public UserService(
            UserRepository userRepository,
            PlayListRepository playListRepository,
            UserMapper userMapper,
            PlaylistMapper playlistMapper) {
        this.userRepository = userRepository;
        this.playListRepository = playListRepository;
        this.userMapper = userMapper;
        this.playlistMapper = playlistMapper;
    }

    /**
     * Loads a user by username for Spring Security authentication.
     *
     * @param username The username identifying the user.
     * @return UserDetails for the user.
     * @throws UsernameNotFoundException if the user is not found.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        logger.info("Loading user by username: {}", username);
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User '" + username + "' not found")
                );
        logger.debug("User loaded successfully: {}", username);
        return UserDetailsImpl.build(user);
    }

    /**
     * Creates a new user with default role and playlist.
     *
     * @param username The username of the new user.
     * @param email    The email of the new user.
     * @param password The encoded password of the new user.
     * @return The created user.
     * @throws UserNotFoundException if the user creation fails.
     */
    @Transactional
    public User createUser(@NotBlank String username, @NotBlank String email, @NotBlank String password) {
        logger.info("Creating user: {}", username);
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRoles(Set.of("USER")); // Default role

        PlayList favorite = new PlayList();
        favorite.setName("Favorites");
        favorite.setDescription("Default playlist for user " + username);
        favorite.setImageUrl("default_image_url");
        PlayList savedPlaylist = playListRepository.save(favorite);

        user.addPlaylist(savedPlaylist);
        User savedUser = userRepository.save(user);
        logger.info("User created successfully: id={}", savedUser.getId());
        return savedUser;
    }

    /**
     * Checks if a user exists by username.
     *
     * @param username The username to check.
     * @return True if the user exists, false otherwise.
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(@NotBlank String username) {
        logger.debug("Checking if username exists: {}", username);
        boolean exists = userRepository.existsByUsername(username);
        logger.debug("Username {} exists: {}", username, exists);
        return exists;
    }

    /**
     * Checks if a user exists by email.
     *
     * @param email The email to check.
     * @return True if the email exists, false otherwise.
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(@NotBlank String email) {
        logger.debug("Checking if email exists: {}", email);
        boolean exists = userRepository.existsByEmail(email);
        logger.debug("Email {} exists: {}", email, exists);
        return exists;
    }

    /**
     * Retrieves user information by username.
     *
     * @param username The username of the user.
     * @return Optional containing the user info, or empty if not found.
     */
    @Transactional(readOnly = true)
    public Optional<UserInfoResponse> getUserInfo(@NotBlank String username) {
        logger.info("Fetching user info for: {}", username);
        return userRepository.findUserByUsername(username)
                .map(userMapper::toUserInfoResponse);
    }

    /**
     * Adds a playlist to a user.
     *
     * @param username The username of the user.
     * @param playlist The playlist to add.
     * @return The created playlist as a DTO.
     * @throws UserNotFoundException if the user is not found.
     */
    @Transactional
    public PlaylistResponse addPlaylistToUser(@NotBlank String username, @Valid PlayList playlist) {
        logger.info("Adding playlist to user: {}", username);
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("User not found: {}", username);
                    return new UserNotFoundException("User with username " + username + " not found");
                });
        PlayList savedPlaylist = playListRepository.save(playlist);
        user.addPlaylist(savedPlaylist);
        userRepository.save(user);
        PlaylistResponse response = playlistMapper.toResponse(savedPlaylist);
        logger.info("Playlist added successfully for user: {}, playlist ID: {}", username, savedPlaylist.getId());
        return response;
    }

    /**
     * Retrieves all playlists for a user.
     *
     * @param username The username of the user.
     * @return List of playlists as DTOs.
     * @throws UserNotFoundException if the user is not found.
     */
    @Transactional(readOnly = true)
    public List<PlaylistResponse> getUserPlaylists(@NotBlank String username) {
        logger.info("Fetching playlists for user: {}", username);
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("User not found: {}", username);
                    return new UserNotFoundException("User with username " + username + " not found");
                });
        List<PlaylistResponse> playlists = user.getPlaylists().stream()
                .map(playlistMapper::toResponse)
                .collect(Collectors.toList());
        logger.debug("Retrieved {} playlists for user: {}", playlists.size(), username);
        return playlists;
    }
}