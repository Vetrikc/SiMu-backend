package org.example.demo2.services;

import jakarta.validation.Valid;
import org.example.demo2.DTO.MusicDTO;

import java.util.List;

public interface MusicService {
    List<MusicDTO> getAllMusic();
    MusicDTO saveMusic(@Valid MusicDTO musicDTO);
    MusicDTO getMusic(Long id);
    void deleteMusic(Long id);
}