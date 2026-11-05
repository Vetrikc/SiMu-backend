package org.example.simu.user.dto;

import java.util.List;

public record PlaylistResponse(Long id, String name, List<String> tracks) {
}
