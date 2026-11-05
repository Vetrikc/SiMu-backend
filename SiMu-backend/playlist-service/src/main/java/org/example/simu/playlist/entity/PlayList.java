package org.example.simu.playlist.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.example.simu.common.jpa.AuditableEntity;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * Playlist aggregate.
 * <p>
 * Tracks are stored as identifiers ({@code playlist_music}) because the {@code music} table lives in
 * another service; the existence of a track is validated through music-service.
 */
@Entity
@Table(name = "playlists")
@SQLRestriction("deleted_at is null")
@Getter
@Setter
public class PlayList extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Name must not be blank")
    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "playlist_music", joinColumns = @JoinColumn(name = "playlist_id"))
    @OrderColumn(name = "position")
    @Column(name = "music_id")
    private List<Long> trackIds = new ArrayList<>();

    public void addTrack(Long musicId) {
        trackIds.add(musicId);
    }
}
