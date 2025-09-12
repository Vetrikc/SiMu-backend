package org.example.demo2.services;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.example.demo2.dto.PlaylistResponse;
import org.example.demo2.entity.Music;
import org.example.demo2.entity.PlayList;
import org.example.demo2.exception.MusicNotFoundException;
import org.example.demo2.exception.PlaylistNotFoundException;
import org.example.demo2.mapper.PlaylistMapper;
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
 * Service implementation for managing playlist-related operations.
 */
@Service
@Validated
public class PlaylistService{

    private static final Logger logger = LoggerFactory.getLogger(PlaylistService.class);

    private final PlayListRepository playListRepository;
    private final MusicRepository musicRepository;
    private final PlaylistMapper playlistMapper;

    @Autowired
    public PlaylistService(
            PlayListRepository playListRepository,
            MusicRepository musicRepository,
            PlaylistMapper playlistMapper) {
        this.playListRepository = playListRepository;
        this.musicRepository = musicRepository;
        this.playlistMapper = playlistMapper;
    }

    /**
     * Creates a new playlist.
     *
     * @param playlist The playlist to create.
     * @return The created playlist as a DTO.
     */
    @Transactional
    public PlaylistResponse createPlayList(@Valid PlayList playlist) {
        logger.info("Creating playlist with name: {}", playlist.getName());
        PlayList savedPlaylist = playListRepository.save(playlist);
        PlaylistResponse response = playlistMapper.toResponse(savedPlaylist);
        logger.info("Playlist created successfully: id={}", response.id());
        return response;
    }

    /**
     * Adds a music track to a playlist.
     *
     * @param playlistId The ID of the playlist.
     * @param musicId The ID of the music track to add.
     * @return The updated playlist as a DTO.
     * @throws PlaylistNotFoundException if the playlist is not found.
     * @throws MusicNotFoundException if the music track is not found.
     */
    @Transactional
    public PlaylistResponse addTrackToPlayList(@Min(1) Long playlistId, @Min(1) Long musicId) {
        logger.info("Adding musicId={} to playlistId={}", musicId, playlistId);
        PlayList playList = playListRepository.findById(playlistId)
                .orElseThrow(() -> {
                    logger.warn("Playlist not found: id={}", playlistId);
                    return new PlaylistNotFoundException("Playlist not found with id: " + playlistId);
                });

        Music music = musicRepository.findById(musicId)
                .orElseThrow(() -> {
                    logger.warn("Music not found: id={}", musicId);
                    return new MusicNotFoundException("Music not found with id: " + musicId);
                });

        if (playList.getTracks().contains(music)) {
            logger.debug("Music id={} already exists in playlist id={}", musicId, playlistId);
            return playlistMapper.toResponse(playList);
        }

        playList.addMusic(music);
        PlayList updatedPlaylist = playListRepository.save(playList);
        logger.info("Music id={} added to playlist id={}", musicId, playlistId);
        return playlistMapper.toResponse(updatedPlaylist);
    }

    /**
     * Retrieves a playlist by its ID.
     *
     * @param id The ID of the playlist.
     * @return Optional containing the playlist DTO, or empty if not found.
     */
    @Transactional(readOnly = true)
    public Optional<PlaylistResponse> getPlaylist(@Min(1) Long id) {
        logger.info("Fetching playlist with id: {}", id);
        return playListRepository.findById(id)
                .map(playlist -> {
                    logger.debug("Retrieved playlist: id={}", id);
                    return playlistMapper.toResponse(playlist);
                });
    }
}