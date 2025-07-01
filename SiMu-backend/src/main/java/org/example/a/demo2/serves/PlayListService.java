package org.example.a.demo2.serves;

import org.example.a.demo2.entity.PlayList;

public interface PlayListService {


    public PlayList createPlayList(String name);

    public void addTrackToPlayList(Long PlayListId, Long musicId);

    public PlayList getPlayList(Long id);
}