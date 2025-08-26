package org.example.demo2.controller;

import jakarta.validation.Valid;
import org.example.demo2.DTO.UserInfoResponse;
import org.example.demo2.entity.PlayList;
import org.example.demo2.entity.User;
import org.example.demo2.services.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing user-related operations.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserServiceImpl userService;

    @GetMapping
    public ResponseEntity<UserInfoResponse> getUserInfo(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Unauthorized access attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserInfoResponse(null));
        }
        logger.debug("Fetching user info for: {}", authentication.getName());
        String username = authentication.getName();
        return ResponseEntity.ok(new UserInfoResponse(username));
    }

    @PostMapping("/playlists")
    public ResponseEntity<User> addNowCreatedPlaylistToUser(Authentication authentication, @Valid @RequestBody PlayList playlist) {
        String username = authentication.getName();
        logger.debug("Adding playlist for user: {}", username);
        User updatedUser = userService.addNowCreatedPlaylistToUser(username, playlist);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/playlists")
    public ResponseEntity<List<PlayList>> getUserPlaylists(Authentication authentication) {
        String username = authentication.getName();
        logger.debug("Fetching playlists for user: {}", username);
        List<PlayList> playlists = userService.getUserPlaylists(username);
        return ResponseEntity.ok(playlists);
    }
}