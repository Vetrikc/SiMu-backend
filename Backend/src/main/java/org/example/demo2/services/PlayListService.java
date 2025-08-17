package org.example.demo2.services;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.demo2.entity.PlayList;

import java.util.Optional;

/**
 * Service interface for managing playlists.
 */
public interface PlayListService {
    PlayList createPlayList(@NotBlank(message = "Name must not be blank")String name, String description);
    PlayList addTrackToPlayList(@NotNull(message = "Playlist ID must not be null") Long playlistId,
                                @NotNull(message = "Music ID must not be null") Long musicId);
    Optional<PlayList> getPlaylist(@NotNull(message = "Playlist ID must not be null") Long id);
}