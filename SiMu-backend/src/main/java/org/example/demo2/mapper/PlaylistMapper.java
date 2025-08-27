package org.example.demo2.mapper;

import org.example.demo2.dto.PlaylistRequest;
import org.example.demo2.dto.PlaylistResponse;
import org.example.demo2.entity.Music;
import org.example.demo2.entity.PlayList;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper interface for converting between Playlist-related DTOs and entities.
 */
@Mapper(componentModel = "spring")
public interface PlaylistMapper {

    /**
     * Converts PlaylistRequest to PlayList entity.
     * Tracks are initialized as an empty list and should be set separately.
     *
     * @param request the PlaylistRequest object
     * @return PlayList entity
     */
    @Mapping(target = "tracks", expression = "java(java.util.Collections.emptyList())")
    PlayList toEntity(PlaylistRequest request);

    /**
     * Converts PlayList entity to PlaylistResponse.
     *
     * @param playlist the PlayList entity
     * @return PlaylistResponse object
     */
    @Mapping(source = "tracks", target = "tracks", qualifiedByName = "mapTracksToIds")
    PlaylistResponse toResponse(PlayList playlist);

    /**
     * Updates an existing PlayList entity with data from PlaylistRequest.
     * Tracks are not updated and should be set separately.
     *
     * @param request the PlaylistRequest object
     * @param playlist the PlayList entity to update
     */
    @Mapping(target = "tracks", ignore = true)
    void updateEntity(PlaylistRequest request, @MappingTarget PlayList playlist);

    /**
     * Converts a list of Music IDs to a list of Music entities.
     *
     * @param trackIds list of music IDs
     * @param musicEntities list of Music entities
     * @return List of Music entities matching the provided IDs
     */
    @Named("mapTrackIdsToEntities")
    default List<Music> mapTrackIdsToEntities(List<String> trackIds, List<Music> musicEntities) {
        if (trackIds == null || musicEntities == null) {
            return Collections.emptyList();
        }
        return trackIds.stream()
                .map(id -> {
                    try {
                        Long musicId = Long.parseLong(id);
                        return musicEntities.stream()
                                .filter(music -> music.getId().equals(musicId))
                                .findFirst()
                                .orElse(null);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(music -> music != null)
                .collect(Collectors.toList());
    }

    /**
     * Converts a list of Music entities to a list of their IDs.
     *
     * @param tracks list of Music entities
     * @return List of music IDs as strings
     */
    @Named("mapTracksToIds")
    default List<String> mapTracksToIds(List<Music> tracks) {
        if (tracks == null) {
            return Collections.emptyList();
        }
        return tracks.stream()
                .map(music -> String.valueOf(music.getId()))
                .collect(Collectors.toList());
    }
}