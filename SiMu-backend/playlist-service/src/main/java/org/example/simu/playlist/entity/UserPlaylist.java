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
import org.example.simu.common.jpa.AuditableEntity;
import org.hibernate.annotations.SQLRestriction;

/**
 * Membership of a playlist in a user's library ({@code user_playlists}).
 * <p>
 * The row is soft-deleted when the user or the playlist is removed, so the library can be audited
 * and restored.
 */
@Entity
@Table(name = "user_playlists",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_playlist", columnNames = {"user_id", "playlist_id"}))
@SQLRestriction("deleted_at is null")
@Getter
@Setter
public class UserPlaylist extends AuditableEntity {

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
