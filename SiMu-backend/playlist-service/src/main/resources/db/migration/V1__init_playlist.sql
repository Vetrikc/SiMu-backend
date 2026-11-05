-- playlist-service, MySQL
CREATE TABLE playlists (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    image_url   VARCHAR(255),
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at  DATETIME(6),
    INDEX ix_playlists_deleted_at (deleted_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ordered list of track ids; music rows live in music-service, so no FK on music_id
CREATE TABLE playlist_music (
    playlist_id BIGINT NOT NULL,
    music_id    BIGINT NOT NULL,
    position    INT    NOT NULL,
    PRIMARY KEY (playlist_id, position),
    CONSTRAINT fk_playlist_music_playlist FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_playlists (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    playlist_id BIGINT      NOT NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at  DATETIME(6),
    UNIQUE KEY uk_user_playlist (user_id, playlist_id),
    INDEX ix_user_playlists_user_id (user_id),
    INDEX ix_user_playlists_deleted_at (deleted_at),
    CONSTRAINT fk_user_playlists_playlist FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
