package org.example.demo2.mapper;

import org.example.demo2.dto.MusicDTO;
import org.example.demo2.dto.MusicUploadDTO;
import org.example.demo2.entity.Music;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

/**
 * Mapper for converting between Music DTOs.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MusicMapper {

    MusicMapper INSTANCE = Mappers.getMapper(MusicMapper.class);

    /**
     * Converts Music entity to MusicDTO.
     *
     * @param music The Music entity.
     * @return MusicDTO.
     */
    MusicDTO toDTO(Music music);

    /**
     * Converts MusicDTO to Music entity.
     *
     * @param musicDTO The MusicDTO.
     * @return Music entity.
     */
    Music toEntity(MusicDTO musicDTO);

    /**
     * Converts MusicUploadDTO to MusicDTO.
     *
     * @param uploadDTO The MusicUploadDTO.
     * @return MusicDTO.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "url", ignore = true)
    MusicDTO toDTO(MusicUploadDTO uploadDTO);

}