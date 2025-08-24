package org.example.demo2.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Entity representing a music track stored in the database.
 */
@Entity
@Table(name = "music")
@Getter
@Setter
public class Music {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Artist must not be blank")
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Music music = (Music) o;
        return Objects.equals(id, music.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}