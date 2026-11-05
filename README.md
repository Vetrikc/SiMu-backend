# SiMu

SiMu is a music streaming project I started while learning Spring Boot.
It was my first larger Spring application: I wrote the backend, an early simple UI, and the music
upload/playback flow. Later I worked on the project together with a frontend developer, so the
repository also contains a React frontend.

The project was never deployed as a public service, but it was useful for learning how a real
application fits together beyond a basic CRUD API.

## Backend architecture (microservices)

The backend used to be a single Spring Boot application. It is now split into five independently
deployable Gradle modules that sit behind an API gateway:

```
Frontend (:5173)
      |
      v
api-gateway (:8081)  -- validates JWT, forwards X-User-Id / X-User-Name / X-User-Roles
      |
      +--> auth-service     (:8082)  /api/v1/auth/**      users, roles, JWT issuing
      +--> user-service     (:8083)  /api/v1/users/**     profile + user playlists (aggregates the two below)
      +--> playlist-service (:8085)  /api/v1/playlists/** playlists, playlist_music, user_playlists
      +--> music-service    (:8084)  /api/v1/music/**     track metadata + S3 upload
```

| Service | Owns the data | Public API | Internal API |
| --- | --- | --- | --- |
| `api-gateway` | – | everything behind `/api/v1/**` | – |
| `auth-service` | `users`, `user_roles` | `/api/v1/auth/signup`, `/api/v1/auth/signin` | `/api/v1/internal/users/{username}` |
| `user-service` | – (aggregator) | `/api/v1/users`, `/api/v1/users/playlists` | – |
| `playlist-service` | `playlists`, `playlist_music`, `user_playlists` | `/api/v1/playlists/**` | `/api/v1/internal/users/{userId}/playlists`, `.../default-playlist` |
| `music-service` | `music` + S3 bucket | `/api/v1/music/**` | `/api/v1/internal/music/{id}` |

Notes:

- every service has its own schema (`simu_auth`, `simu_music`, `simu_playlist`);
- services never read each other's tables, they call each other over HTTP;
- `/api/v1/internal/**` is not routed by the gateway, so it is unreachable from the internet;
- the public contract (paths, payloads, status codes) is the same as in the monolith, so the
  React frontend works without changes.

## Repository structure

- `SiMu-backend/` — Gradle multi-module backend (`api-gateway`, `auth-service`, `user-service`, `music-service`, `playlist-service`)
- `Frontend/` — React frontend
- `SiMu/` — the very first Maven sketch of the project, kept for history

## Backend features

- user registration and authentication;
- JWT-based security shared by the gateway and the services;
- music upload and download/streaming;
- playlists;
- user and music management;
- S3-compatible object storage for audio files;
- MySQL persistence, one schema per service;
- Swagger/OpenAPI documentation in every service.

## Stack

Backend: Java 21, Spring Boot 3.4, Spring Cloud Gateway, Spring Security, Spring Data JPA, MySQL,
JWT, S3 API, MapStruct, Lombok, Gradle and Docker.

Frontend: React, TypeScript and Vite.

## Running the backend

Create the environment file next to `docker-compose.yml`:

```bash
cd SiMu-backend
cp .env.example .env
```

Fill in the database password, the S3 credentials and a JWT secret (at least 32 characters), then:

```bash
docker compose up --build
```

Swagger UI:

| Service | URL |
| --- | --- |
| API gateway | http://localhost:8081 |
| auth-service | http://localhost:8082/swagger-ui.html |
| user-service | http://localhost:8083/swagger-ui.html |
| music-service | http://localhost:8084/swagger-ui.html |
| playlist-service | http://localhost:8085/swagger-ui.html |

## Running without Docker

Every service can be started on its own (Java 21 required, Gradle wrapper downloads the rest):

```bash
cd SiMu-backend
./gradlew :auth-service:bootRun      # and the other modules in separate terminals
```

The services read their configuration from environment variables (`AUTH_DB_*`, `MUSIC_DB_*`,
`PLAYLIST_DB_*`, `JWT_SECRET`, `S3_*`, `*_SERVICE_URL`); see `.env.example` for the full list.

## Environment variables

| Variable | Used by | Description |
| --- | --- | --- |
| `GATEWAY_PORT` | docker-compose | published gateway port (a frontend expects 8081) |
| `AUTH_SERVICE_PORT`, `USER_SERVICE_PORT`, `MUSIC_SERVICE_PORT`, `PLAYLIST_SERVICE_PORT` | docker-compose | published service ports |
| `MYSQL_PORT`, `MYSQL_ROOT_PASSWORD` | docker-compose | MySQL access |
| `AUTH_DB_NAME`, `MUSIC_DB_NAME`, `PLAYLIST_DB_NAME` | auth / music / playlist service | schema names created by `docker/mysql/init` |
| `JWT_SECRET` | gateway + all services | HS256 secret, must be the same everywhere |
| `JWT_EXPIRATION_MS` | auth-service | token lifetime (default ~41 days, as before) |
| `S3_ENDPOINT`, `S3_REGION`, `S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY` | music-service | object storage |
| `CORS_ALLOWED_ORIGINS` | api-gateway | comma separated list of allowed origins |
| `AUTH_SERVICE_URL`, `USER_SERVICE_URL`, `MUSIC_SERVICE_URL`, `PLAYLIST_SERVICE_URL` | gateway / services | base URLs of the downstream services |

## Project status

Archived as a monolith; the microservice split lives on the `feature/microservices` branch.
