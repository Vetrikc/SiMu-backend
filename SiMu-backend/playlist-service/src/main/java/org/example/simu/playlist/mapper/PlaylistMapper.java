package org.example.simu.playlist.mapper;

import org.example.simu.playlist.dto.PlaylistResponse;
import org.example.simu.playlist.entity.PlayList;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlaylistMapper {

    @Mapping(source = "trackIds", target = "tracks", qualifiedByName = "idsToStrings")
    PlaylistResponse toResponse(PlayList playList);

    @Named("idsToStrings")
    default List<String> idsToStrings(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().map(String::valueOf).toList();
    }
}
