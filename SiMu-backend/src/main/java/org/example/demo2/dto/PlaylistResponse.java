package org.example.demo2.dto;

import java.util.List;

public record PlaylistResponse(Long id, String name, List<String> tracks) {}