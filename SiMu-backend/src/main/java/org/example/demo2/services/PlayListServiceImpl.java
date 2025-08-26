package org.example.demo2.services;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.demo2.entity.Music;
import org.example.demo2.entity.PlayList;
import org.example.demo2.repository.MusicRepository;
import org.example.demo2.repository.PlayListRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

/**
 * Service implementation for managing playlists.
 */
@Service
@Validated
public class PlayListServiceImpl implements PlayListService {
    private static final Logger logger = LoggerFactory.getLogger(PlayListServiceImpl.class);

    @Autowired
    private PlayListRepository playListRepository;

    @Autowired
    private MusicRepository musicRepository;

    @Override
    @Transactional
    public PlayList createPlayList(@NotBlank(message = "Name must not be blank") String name, String description) {
        logger.debug("Creating playlist with name: {}", name);
        PlayList playList = new PlayList();
        playList.setName(name);
        playList.setDescription(description);
        PlayList savedPlaylist = playListRepository.save(playList);
        logger.info("Playlist created successfully: id={}", savedPlaylist.getId());
        return savedPlaylist;
    }

    @Override
    @Transactional
    public PlayList addTrackToPlayList(@NotNull(message = "Playlist ID must not be null") Long playlistId,
                                       @NotNull(message = "Music ID must not be null") Long musicId) {
        logger.debug("Adding musicId={} to playlistId={}", musicId, playlistId);
        PlayList playList = playListRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found with id: " + playlistId));
        Music music = musicRepository.findById(musicId)
                .orElseThrow(() -> new IllegalArgumentException("Music not found with id: " + musicId));

        if (playList.getTracks().contains(music)) {
            logger.debug("Music id={} already exists in playlist id={}", musicId, playlistId);
            return playList;
        }

        playList.addMusic(music);
        PlayList updatedPlaylist = playListRepository.save(playList);
        logger.info("Music id={} added to playlist id={}", musicId, playlistId);
        return updatedPlaylist;
    }

    @Override
    public Optional<PlayList> getPlaylist(@NotNull(message = "Playlist ID must not be null") Long id) {
        logger.debug("Fetching playlist with id: {}", id);
        return playListRepository.findById(id);
    }
}