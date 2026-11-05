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

    /**
     * Adds a track to a playlist. The track is validated in {@code music-service} first, because the
     * {@code music} table is no longer part of this database.
     */
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

    /**
     * Creates a playlist and links it to the user in one local transaction - both tables belong to
     * this service, so no distributed transaction is required.
     */
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
