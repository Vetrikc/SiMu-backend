package org.example.simu.playlist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Membership of a playlist in a user's library ({@code user_playlists} table).
 * <p>
 * The table used to be the join table of {@code User.playlists} in the monolith; it is kept with the
 * same name so that the contract of {@code /api/v1/users/playlists} does not change.
 */
@Entity
@Table(name = "user_playlists",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_playlist", columnNames = {"user_id", "playlist_id"}))
@Getter
@Setter
public class UserPlaylist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    public UserPlaylist() {
    }

    public UserPlaylist(Long userId, Long playlistId) {
        this.userId = userId;
        this.playlistId = playlistId;
    }
}
