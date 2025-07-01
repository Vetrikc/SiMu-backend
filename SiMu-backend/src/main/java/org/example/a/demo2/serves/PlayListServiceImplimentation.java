package org.example.a.demo2.serves;

import org.example.a.demo2.DAO.MusicRepository;

import org.example.a.demo2.DAO.PlayListRepository;
import org.example.a.demo2.entity.Music;
import org.example.a.demo2.entity.PlayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlayListServiceImplimentation implements PlayListService {
    @Autowired
    private PlayListRepository playListRepository;
    @Autowired
    private MusicRepository musicRepository;

    public PlayList createPlayList(String name) {
        PlayList PlayList = new PlayList();
        PlayList.setName(name);
        return playListRepository.save(PlayList);
    }

    public void addTrackToPlayList(Long PlayListId, Long musicId) {
        PlayList PlayList = playListRepository.findById(PlayListId)
            .orElseThrow(() -> new RuntimeException("PlayList not found"));
        Music music = musicRepository.findById(musicId)
            .orElseThrow(() -> new RuntimeException("Music not found"));
        
        PlayList.getMusics().add(music);
        playListRepository.save(PlayList);
    }

    public PlayList getPlayList(Long id) {
        return playListRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("PlayList not found"));
    }
}