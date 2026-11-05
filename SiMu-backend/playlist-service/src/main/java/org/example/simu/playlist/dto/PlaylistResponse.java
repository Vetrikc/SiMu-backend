package org.example.simu.playlist.dto;

import java.util.List;

public record PlaylistResponse(Long id, String name, List<String> tracks) {
}
