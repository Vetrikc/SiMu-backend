-- MySQL is used by the services that keep music and playlists.
-- auth-service and user-service run on PostgreSQL (see auth-db / user-db in docker-compose.yml).
CREATE DATABASE IF NOT EXISTS simu_music CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS simu_playlist CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
