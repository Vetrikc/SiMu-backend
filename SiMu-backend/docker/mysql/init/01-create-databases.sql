-- One schema per service: services never share tables.
CREATE DATABASE IF NOT EXISTS simu_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS simu_music CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS simu_playlist CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
