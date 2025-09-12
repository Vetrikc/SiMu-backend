package org.example.demo2.services;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.example.demo2.dto.MusicDTO;
import org.example.demo2.entity.Music;
import org.example.demo2.exception.MusicNotFoundException;
import org.example.demo2.mapper.MusicMapper;
import org.example.demo2.repository.MusicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service implementation for managing music entities.
 */
@Service
@Validated
public class MusicService {

    private static final Logger logger = LoggerFactory.getLogger(MusicService.class);

    private final MusicRepository musicRepository;
    private final MusicMapper musicMapper;

    @Autowired
    public MusicService(MusicRepository musicRepository, MusicMapper musicMapper) {
        this.musicRepository = musicRepository;
        this.musicMapper = musicMapper;
    }

    /**
     * Retrieves all music records.
     *
     * @return List of music DTOs.
     */
    @Transactional(readOnly = true)
    public List<MusicDTO> getAllMusic() {
        logger.info("Fetching all music records");
        List<Music> musicList = musicRepository.findAll();
        List<MusicDTO> result = musicList.stream()
                .map(musicMapper::toDTO)
                .collect(Collectors.toList());
        logger.debug("Retrieved {} music records", result.size());
        return result;
    }

    /**
     * Retrieves a music record by its ID.
     *
     * @param id The ID of the music record.
     * @return Optional containing the music DTO, or empty if not found.
     */
    @Transactional(readOnly = true)
    public Optional<MusicDTO> getMusic(@Min(1) Long id) {
        logger.info("Fetching music with ID: {}", id);
        return musicRepository.findById(id)
                .map(music -> {
                    logger.debug("Retrieved music: id={}", id);
                    return musicMapper.toDTO(music);
                });
    }

    /**
     * Saves a music record.
     *
     * @param musicDTO The music DTO to save.
     * @return Saved music DTO.
     */
    @Transactional
    public MusicDTO saveMusic(@Valid MusicDTO musicDTO) {
        logger.info("Saving music: name={}, artist={}", musicDTO.name(), musicDTO.artist());
        Music music = musicMapper.toEntity(musicDTO);
        Music savedMusic = musicRepository.save(music);
        MusicDTO result = musicMapper.toDTO(savedMusic);
        logger.info("Music saved successfully: id={}", result.id());
        return result;
    }

    /**
     * Deletes a music record by its ID.
     *
     * @param id The ID of the music record to delete.
     * @throws MusicNotFoundException if the music record is not found.
     */
    @Transactional
    public void deleteMusic(@Min(1) Long id) {
        logger.info("Deleting music with ID: {}", id);
        if (!musicRepository.existsById(id)) {
            logger.warn("Music not found for deletion: id={}", id);
            throw new MusicNotFoundException("Music not found with ID: " + id);
        }
        musicRepository.deleteById(id);
        logger.info("Music deleted successfully: id={}", id);
    }
}