package org.example.a.demo2.controller;

import org.example.a.demo2.entity.PlayList;
import org.example.a.demo2.serves.PlayListServiceImplimentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {
    @Autowired
    private PlayListServiceImplimentation playlistServiceImplimentation;

    @PostMapping
    public PlayList createPlaylist(@RequestParam String name) {
        return playlistServiceImplimentation.createPlayList(name);
    }

    @PostMapping("/{playlistId}/tracks/{musicId}")
    public void addTrackToPlaylist(@PathVariable Long playlistId, @PathVariable Long musicId) {
        playlistServiceImplimentation.addTrackToPlayList(playlistId, musicId);
    }

    @GetMapping("/{id}")
    public PlayList getPlaylist(@PathVariable Long id) {
        return playlistServiceImplimentation.getPlayList(id);
    }
}