package org.example.simu.music.service;

import org.example.simu.music.config.CacheConfig;
import org.example.simu.music.dto.MusicDTO;
import org.example.simu.music.dto.PageResponse;
import org.example.simu.music.entity.Music;
import org.example.simu.music.exception.MusicNotFoundException;
import org.example.simu.music.mapper.MusicMapper;
import org.example.simu.music.repository.MusicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
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

    @Cacheable(cacheNames = CacheConfig.MUSIC_PAGE, key = "#cursor + ':' + #limit")
    @Transactional(readOnly = true)
    public PageResponse<MusicDTO> getPage(Long cursor, int limit) {
        Slice<Music> slice = musicRepository.findPage(cursor, PageRequest.of(0, limit));
        return toPage(slice);
    }

    @Cacheable(cacheNames = CacheConfig.MUSIC_SEARCH, key = "#q.toLowerCase() + ':' + #cursor + ':' + #limit")
    @Transactional(readOnly = true)
    public PageResponse<MusicDTO> search(String q, Long cursor, int limit) {
        Slice<Music> slice = musicRepository.search(q, cursor, PageRequest.of(0, limit));
        return toPage(slice);
    }

    private PageResponse<MusicDTO> toPage(Slice<Music> slice) {
        List<Music> content = slice.getContent();
        List<MusicDTO> items = content.stream().map(musicMapper::toDto).toList();
        Long nextCursor = slice.hasNext() && !content.isEmpty()
                ? content.get(content.size() - 1).getId()
                : null;
        return new PageResponse<>(items, nextCursor, slice.hasNext());
    }

    @Transactional(readOnly = true)
    public MusicDTO getMusic(Long id) {
        return musicRepository.findById(id)
                .map(musicMapper::toDto)
                .orElseThrow(() -> new MusicNotFoundException("Music record not found with ID: " + id));
    }

    @CacheEvict(cacheNames = {CacheConfig.MUSIC_PAGE, CacheConfig.MUSIC_SEARCH}, allEntries = true)
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
    @CacheEvict(cacheNames = {CacheConfig.MUSIC_PAGE, CacheConfig.MUSIC_SEARCH}, allEntries = true)
    @Transactional
    public void deleteMusic(Long id) {
        Music music = musicRepository.findById(id)
                .orElseThrow(() -> new MusicNotFoundException("Music record not found with ID: " + id));
        music.markDeleted();
        musicRepository.save(music);
        log.info("Music soft-deleted: id={}", id);
    }
}
