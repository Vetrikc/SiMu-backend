package org.example.simu.music.service;

import org.example.simu.music.dto.MusicDTO;
import org.example.simu.music.entity.Music;
import org.example.simu.music.exception.MusicNotFoundException;
import org.example.simu.music.mapper.MusicMapper;
import org.example.simu.music.repository.MusicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MusicService {

    private static final Logger log = LoggerFactory.getLogger(MusicService.class);

    private final MusicRepository musicRepository;
    private final MusicMapper musicMapper;

    public MusicService(MusicRepository musicRepository, MusicMapper musicMapper) {
        this.musicRepository = musicRepository;
        this.musicMapper = musicMapper;
    }

    @Transactional(readOnly = true)
    public List<MusicDTO> getAllMusic() {
        List<MusicDTO> result = musicRepository.findAll().stream()
                .map(musicMapper::toDto)
                .toList();
        log.debug("Retrieved {} music records", result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public MusicDTO getMusic(Long id) {
        return musicRepository.findById(id)
                .map(musicMapper::toDto)
                .orElseThrow(() -> new MusicNotFoundException("Music record not found with ID: " + id));
    }

    @Transactional
    public MusicDTO saveMusic(MusicDTO musicDTO) {
        Music saved = musicRepository.save(musicMapper.toEntity(musicDTO));
        log.info("Music saved with id={}", saved.getId());
        return musicMapper.toDto(saved);
    }

    /**
     * Soft delete: the row is kept because playlists in other services reference the id and the
     * audio object stays restorable.
     */
    @Transactional
    public void deleteMusic(Long id) {
        Music music = musicRepository.findById(id)
                .orElseThrow(() -> new MusicNotFoundException("Music record not found with ID: " + id));
        music.markDeleted();
        musicRepository.save(music);
        log.info("Music soft-deleted: id={}", id);
    }
}
