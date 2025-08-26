package org.example.demo2.DTO;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for user information response.
 */
@Getter
@Setter
public class UserInfoResponse {
    private String username;

    public UserInfoResponse(String username) {
        this.username = username;
    }
}