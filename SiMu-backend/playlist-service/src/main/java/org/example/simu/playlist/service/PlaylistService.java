package org.example.simu.playlist.service;

import org.example.simu.playlist.client.MusicServiceClient;
import org.example.simu.playlist.dto.MusicDTO;
import org.example.simu.playlist.dto.PlaylistResponse;
import org.example.simu.playlist.entity.PlayList;
import org.example.simu.playlist.entity.UserPlaylist;
import org.example.simu.playlist.exception.PlaylistNotFoundException;
import org.example.simu.playlist.mapper.PlaylistMapper;
import org.example.simu.playlist.repository.PlayListRepository;
import org.example.simu.playlist.repository.UserPlaylistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Owns the {@code playlists}, {@code playlist_music} and {@code user_playlists} tables.
 * Deleting is always a soft delete.
 */
@Service
public class PlaylistService {

    private static final Logger log = LoggerFactory.getLogger(PlaylistService.class);

    private final PlayListRepository playListRepository;
    private final UserPlaylistRepository userPlaylistRepository;
    private final PlaylistMapper playlistMapper;
    private final MusicServiceClient musicServiceClient;

    public PlaylistService(PlayListRepository playListRepository,
                           UserPlaylistRepository userPlaylistRepository,
                           PlaylistMapper playlistMapper,
                           MusicServiceClient musicServiceClient) {
        this.playListRepository = playListRepository;
        this.userPlaylistRepository = userPlaylistRepository;
        this.playlistMapper = playlistMapper;
        this.musicServiceClient = musicServiceClient;
    }

    @Transactional
    public PlaylistResponse createPlaylist(String name, String description) {
        PlayList playList = new PlayList();
        playList.setName(name);
        playList.setDescription(description);
        PlayList saved = playListRepository.save(playList);
        log.info("Playlist created: id={}", saved.getId());
        return playlistMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Optional<PlaylistResponse> getPlaylist(Long id) {
        return playListRepository.findById(id).map(playlistMapper::toResponse);
    }

    @Transactional
    public PlaylistResponse addTrackToPlaylist(Long playlistId, Long musicId) {
        PlayList playList = playListRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistNotFoundException("Playlist not found with ID: " + playlistId));

        MusicDTO music = musicServiceClient.getMusic(musicId);

        if (playList.getTrackIds().contains(music.id())) {
            log.debug("Music id={} already exists in playlist id={}", musicId, playlistId);
            return playlistMapper.toResponse(playList);
        }

        playList.addTrack(music.id());
        PlaylistResponse response = playlistMapper.toResponse(playListRepository.save(playList));
        log.info("Music id={} added to playlist id={}", musicId, playlistId);
        return response;
    }

    /**
     * Soft-deletes the playlist and every library link that points at it.
     */
    @Transactional
    public void deletePlaylist(Long playlistId) {
        PlayList playList = playListRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistNotFoundException("Playlist not found with ID: " + playlistId));

        playList.markDeleted();
        playListRepository.save(playList);

        userPlaylistRepository.findByPlaylistId(playlistId).forEach(link -> {
            link.markDeleted();
            userPlaylistRepository.save(link);
        });

        log.info("Playlist soft-deleted: id={}", playlistId);
    }

    /**
     * Reacts to a {@code UserDeleted} Kafka event: the user's playlists and library links are
     * soft-deleted in one local transaction.
     */
    @Transactional
    public void softDeleteUserLibrary(Long userId) {
        List<UserPlaylist> links = userPlaylistRepository.findByUserIdOrderByIdAsc(userId);
        if (links.isEmpty()) {
            return;
        }

        links.forEach(link -> {
            link.markDeleted();
            userPlaylistRepository.save(link);
            playListRepository.findById(link.getPlaylistId()).ifPresent(playList -> {
                playList.markDeleted();
                playListRepository.save(playList);
            });
        });

        log.info("Library of user id={} soft-deleted ({} playlists)", userId, links.size());
    }

    @Transactional(readOnly = true)
    public List<PlaylistResponse> getPlaylistsForUser(Long userId) {
        List<Long> playlistIds = userPlaylistRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(UserPlaylist::getPlaylistId)
                .toList();

        if (playlistIds.isEmpty()) {
            return List.of();
        }

        Map<Long, PlayList> byId = playListRepository.findAllById(playlistIds).stream()
                .collect(Collectors.toMap(PlayList::getId, Function.identity()));

        return playlistIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(playlistMapper::toResponse)
                .toList();
    }

    @Transactional
    public PlaylistResponse createPlaylistForUser(Long userId, String name, String description) {
        PlayList playList = new PlayList();
        playList.setName(name);
        playList.setDescription(description);
        PlayList saved = playListRepository.save(playList);
        userPlaylistRepository.save(new UserPlaylist(userId, saved.getId()));
        log.info("Playlist id={} created for user id={}", saved.getId(), userId);
        return playlistMapper.toResponse(saved);
    }

    /**
     * Replaces the {@code "Favorites"} playlist the monolith created during signup.
     */
    @Transactional
    public PlaylistResponse createDefaultPlaylistForUser(Long userId) {
        PlayList playList = new PlayList();
        playList.setName("Favorites");
        playList.setDescription("Default playlist for user " + userId);
        playList.setImageUrl("default_image_url");
        PlayList saved = playListRepository.save(playList);
        userPlaylistRepository.save(new UserPlaylist(userId, saved.getId()));
        log.info("Default playlist id={} created for user id={}", saved.getId(), userId);
        return playlistMapper.toResponse(saved);
    }
}
