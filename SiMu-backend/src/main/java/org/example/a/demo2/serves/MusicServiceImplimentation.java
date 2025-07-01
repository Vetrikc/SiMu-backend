package org.example.a.demo2.serves;

import jakarta.persistence.EntityNotFoundException;
import org.example.a.demo2.DAO.MusicRepository;
import org.example.a.demo2.entity.Music;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MusicServiceImplimentation implements MusicServes {

    @Autowired
    private MusicRepository musicRepository;

    @Override
    public List<Music> getAllMusic() {
        return musicRepository.findAll();
    }

    @Override
    public void seveMusic(Music music) {
        musicRepository.save(music);
    }

    @Override
    public Music getMusic(Long id) {
        return musicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Music not found for id: " + id));
    }

    @Override
    public void deleteMusic(Music music) {
        musicRepository.delete(music);
    }
}
