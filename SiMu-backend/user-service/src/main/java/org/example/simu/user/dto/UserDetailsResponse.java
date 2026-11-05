package org.example.simu.user.dto;

import java.util.Set;

public record UserDetailsResponse(Long id, String username, String email, Set<String> roles) {
}
