package org.example.simu.playlist.dto;

import java.io.Serializable;
import java.util.List;

public record PlaylistResponse(Long id, String name, List<String> tracks) implements Serializable {
}