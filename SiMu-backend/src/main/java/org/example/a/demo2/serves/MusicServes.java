package org.example.a.demo2.serves;

import org.example.a.demo2.entity.Music;

import java.util.List;

public interface MusicServes {
    public List<Music> getAllMusic();

    public void seveMusic(Music music);

    public Music getMusic(Long id);

    public void deleteMusic(Music music);
}
