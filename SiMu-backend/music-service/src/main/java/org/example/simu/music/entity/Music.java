package org.example.simu.music.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.example.simu.common.jpa.AuditableEntity;
import org.hibernate.annotations.SQLRestriction;

import java.util.Objects;

/**
 * Track metadata. The audio file itself lives in S3/MinIO storage, referenced by {@code url}.
 * <p>
 * Deleting a track only marks the row: playlists of other users keep referring to its id and the
 * audio object can still be restored.
 */
@Entity
@Table(name = "music")
@SQLRestriction("deleted_at is null")
@Getter
@Setter
public class Music extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Name must not be blank")
    @Column(name = "name")
    private String name;

    @NotBlank(message = "Artist must not be blank")
    @Column(name = "artist")
    private String artist;

    @NotBlank(message = "URL must not be blank")
    @Column(name = "url")
    private String url;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Music music = (Music) o;
        return Objects.equals(id, music.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
