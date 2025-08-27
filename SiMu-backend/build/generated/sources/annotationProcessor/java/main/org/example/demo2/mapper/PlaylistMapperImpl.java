package org.example.demo2.mapper;

import java.util.List;
import javax.annotation.processing.Generated;
import org.example.demo2.dto.PlaylistRequest;
import org.example.demo2.dto.PlaylistResponse;
import org.example.demo2.entity.PlayList;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-27T02:48:20+0300",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.13.jar, environment: Java 21.0.8 (IBM Corporation)"
)
@Component
public class PlaylistMapperImpl implements PlaylistMapper {

    @Override
    public PlayList toEntity(PlaylistRequest request) {
        if ( request == null ) {
            return null;
        }

        PlayList playList = new PlayList();

        playList.setName( request.name() );

        playList.setTracks( java.util.Collections.emptyList() );

        return playList;
    }

    @Override
    public PlaylistResponse toResponse(PlayList playlist) {
        if ( playlist == null ) {
            return null;
        }

        List<String> tracks = null;
        Long id = null;
        String name = null;

        tracks = mapTracksToIds( playlist.getTracks() );
        id = playlist.getId();
        name = playlist.getName();

        PlaylistResponse playlistResponse = new PlaylistResponse( id, name, tracks );

        return playlistResponse;
    }

    @Override
    public void updateEntity(PlaylistRequest request, PlayList playlist) {
        if ( request == null ) {
            return;
        }

        playlist.setName( request.name() );
    }
}
