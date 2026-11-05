package org.example.simu.music.mapper;

import org.example.simu.music.dto.MusicDTO;
import org.example.simu.music.entity.Music;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MusicMapper {

    MusicDTO toDto(Music music);

    Music toEntity(MusicDTO musicDTO);
}
