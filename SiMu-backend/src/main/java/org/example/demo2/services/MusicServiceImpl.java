package org.example.demo2.services;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.example.demo2.DTO.MusicDTO;
import org.example.demo2.entity.Music;
import org.example.demo2.repository.MusicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for managing music entities.
 */
@Service
@Validated
public class MusicServiceImpl implements MusicService {
    private static final Logger logger = LoggerFactory.getLogger(MusicServiceImpl.class);

    @Autowired
    private MusicRepository musicRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MusicDTO> getAllMusic() {
        logger.debug("Fetching all music records");
        return musicRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MusicDTO saveMusic(@Valid MusicDTO musicDTO) {
        Objects.requireNonNull(musicDTO, "MusicDTO must not be null");
        logger.info("Saving music: name={}, artist={}", musicDTO.getName(), musicDTO.getArtist());
        Music music = toEntity(musicDTO);
        Music savedMusic = musicRepository.save(music);
        return toDTO(savedMusic);
    }

    @Override
    @Transactional(readOnly = true)
    public MusicDTO getMusic(Long id) {
        Objects.requireNonNull(id, "Music ID must not be null");
        logger.debug("Fetching music with id: {}", id);
        Music music = musicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Music not found for id: " + id));
        return toDTO(music);
    }

    @Override
    @Transactional
    public void deleteMusic(Long id) {
        Objects.requireNonNull(id, "Music ID must not be null");
        if (!musicRepository.existsById(id)) {
            throw new EntityNotFoundException("Music not found for id: " + id);
        }
        logger.info("Deleting music with id: {}", id);
        musicRepository.deleteById(id);
    }

    private MusicDTO toDTO(Music music) {
        MusicDTO musicDTO = new MusicDTO();
        musicDTO.setId(music.getId());
        musicDTO.setName(music.getName());
        musicDTO.setArtist(music.getArtist());
        musicDTO.setUrl(music.getUrl());
        return musicDTO;
    }

    private Music toEntity(MusicDTO musicDTO) {
        Music music = new Music();
        music.setId(musicDTO.getId());
        music.setName(musicDTO.getName());
        music.setArtist(musicDTO.getArtist());
        music.setUrl(musicDTO.getUrl());
        return music;
    }
}