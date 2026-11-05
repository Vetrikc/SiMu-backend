# SiMu

SiMu is a music streaming project I started while learning Spring Boot. Later I worked on it with a
frontend developer, so the repository contains both the backend and a React frontend.

The backend used to be a single Spring Boot application; it is now a set of independently deployable
services with a gateway, a shared identity directory, caching, an event stream and S3-compatible
object storage.

## Architecture

```
Frontend (:5173)
      |
      v
api-gateway (:8081) -- validates JWT, strips client identity headers, forwards
      |                X-User-Id / X-User-Name / X-User-Roles
      +--> auth-service     (:8082)  /api/v1/auth/**        users, roles, JWT, outbox -> Kafka
      +--> user-service     (:8083)  /api/v1/users/**       profile read model (Kafka), Redis cache
      +--> playlist-service (:8085)  /api/v1/playlists/**   playlists, library links, Kafka consumer
      +--> music-service    (:8084)  /api/v1/music/**       track metadata + S3/MinIO upload

infrastructure (docker-compose):
  PostgreSQL 16  -- auth-db (:5434), user-db (:5435)     one database per service
  MySQL 8        -- music + playlist data
  Redis 7        -- cache of the auth and user read paths
  Kafka 3.9      -- topic simu.user.events (auth -> user, auth -> playlist)
  MinIO          -- local S3-compatible storage for audio files (:9000, console :9001)
```

| Service | Owns | Public API | Internal API |
| --- | --- | --- | --- |
| `api-gateway` | – | everything behind `/api/v1/**` | – |
| `auth-service` | PostgreSQL `users`, `user_roles`, `outbox_events` | `/api/v1/auth/signup`, `/signin` | `GET/DELETE /api/v1/internal/users/**` |
| `user-service` | PostgreSQL `user_profiles`, `processed_events` | `/api/v1/users`, `/users/playlists`, `DELETE /users/me` | – |
| `playlist-service` | MySQL `playlists`, `playlist_music`, `user_playlists` | `/api/v1/playlists/**` | `/api/v1/internal/users/{userId}/playlists` |
| `music-service` | MySQL `music` + S3 bucket | `/api/v1/music/**` | `/api/v1/internal/music/{id}` |
| `common` | – (library) | UUID v7 generator, auditable base entity, Kafka event contracts | – |

### Why these pieces

**Local S3 with MinIO.** The music service speaks the S3 API only; `S3_ENDPOINT` decides where the
files really go. Locally that is MinIO: the bucket is created on start and made public-read, so the
browser can stream tracks straight from `http://localhost:9000/simu-music/...`. The SDK is configured
with `forcePathStyle=true`, which MinIO requires. Pointing `S3_ENDPOINT`/`S3_PUBLIC_URL` at a real
S3-compatible provider (Timeweb, AWS, Cloudflare R2) is a configuration-only change.

**PostgreSQL for auth + user.** The identity tables moved to PostgreSQL (`auth-db`), and user-service
got its own database (`user-db`) for the profile read model. Each service has its own server, schema
and Flyway migrations - no shared tables.

**Redis caching.** Both read paths are cached: `auth:users` (identity lookups made by other services)
and `user:profiles` (profile responses). Entries live 10 minutes and are evicted on write/delete or
when a Kafka event changes the data. The cache holds a typed JSON payload, so Redis never has to
deserialise arbitrary classes.

**Kafka between auth and the rest.** Signup and account deletion are published as events on
`simu.user.events`. user-service keeps its profile projection in sync; playlist-service removes the
library of a deleted account. Delivery is guaranteed by a **transactional outbox**: the event row is
committed in the same transaction as the user change and a scheduler drains the outbox into Kafka,
so a committed user change can never lose its event.

**UUID v7.** Used where a new identifier is generated inside the system and does not have to satisfy
an old contract: event ids, outbox rows, and the surrogate key of the profile projection. v7 is
time-ordered, so indexes stay append-only instead of being scattered like random v4 UUIDs. Public
entity ids (`users`, `playlists`, `music`) stay `BIGINT`, because they are part of the OpenAPI
contract the React frontend was generated from - changing them would break the client for no gain.

**Soft delete.** `users`, `music`, `playlists`, `user_playlists` and `user_profiles` are never physically
removed: they carry `deleted_at` and are annotated with `@SQLRestriction("deleted_at is null")`, so a
deleted row disappears from every generated query but stays available for audit and recovery.
`DELETE /api/v1/users/me`, `DELETE /api/v1/music/{id}` and `DELETE /api/v1/playlists/{id}` follow that
rule; account deletion also propagates through Kafka to the other services.

**Audit columns.** Every business table has `created_at`, `updated_at` and `deleted_at`, filled by
JPA lifecycle callbacks (mapped superclass in `common`).

## Repository structure

- `SiMu-backend/` — Gradle multi-module backend
  - `common/` — UUID v7, `AuditableEntity`, Kafka event contracts
  - `api-gateway/`, `auth-service/`, `user-service/`, `music-service/`, `playlist-service/`
  - `docker-compose.yml`, `.env.example`, `docker/mysql/init/`
- `Frontend/` — React frontend
- `SiMu/` — the very first Maven sketch of the project, kept for history

## Running the backend

```bash
cd SiMu-backend
cp .env.example .env      # fill in passwords and a JWT secret (>= 32 chars)
docker compose up --build
```

| What | URL |
| --- | --- |
| API gateway | http://localhost:8081 |
| auth-service Swagger | http://localhost:8082/swagger-ui.html |
| user-service Swagger | http://localhost:8083/swagger-ui.html |
| music-service Swagger | http://localhost:8084/swagger-ui.html |
| playlist-service Swagger | http://localhost:8085/swagger-ui.html |
| MinIO console | http://localhost:9001 (user/password from `.env`) |
| auth database | `localhost:5434`, db `simu_auth` |
| user database | `localhost:5435`, db `simu_user` |
| Kafka (from the host) | `localhost:9094` |

Run a service outside Docker: `./gradlew :auth-service:bootRun` (needs the infrastructure containers
and the env variables below).

## Database migrations

Flyway owns the schema of every service (`src/main/resources/db/migration`), Hibernate runs in
`validate` mode. A new field means a new migration file, not a silent `ddl-auto=update`.

## Environment variables

| Variable | Used by | Description |
| --- | --- | --- |
| `GATEWAY_PORT`, `AUTH_SERVICE_PORT`, `USER_SERVICE_PORT`, `MUSIC_SERVICE_PORT`, `PLAYLIST_SERVICE_PORT` | compose | published ports |
| `MYSQL_PORT`, `MYSQL_ROOT_PASSWORD` | compose | MySQL for music + playlist |
| `MUSIC_DB_NAME`, `PLAYLIST_DB_NAME` | music/playlist service | MySQL schema names |
| `AUTH_DB_*` | auth-service | PostgreSQL connection (host, port, name, user, password) |
| `USER_DB_*` | user-service | PostgreSQL connection |
| `REDIS_HOST`, `REDIS_PORT` | auth/user service | cache |
| `KAFKA_BOOTSTRAP_SERVERS` | auth/user/playlist service | `kafka:9092` inside compose |
| `JWT_SECRET`, `JWT_EXPIRATION_MS` | gateway + all services | HS256 secret and token lifetime |
| `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD` | MinIO | local object storage credentials |
| `S3_BUCKET`, `S3_REGION`, `S3_PUBLIC_URL` | music-service, MinIO | bucket and the URL the browser uses |
| `S3_ENDPOINT`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_PATH_STYLE` | music-service | any S3-compatible provider |
| `CORS_ALLOWED_ORIGINS` | gateway | comma separated origins |
| `AUTH_SERVICE_URL`, `USER_SERVICE_URL`, `MUSIC_SERVICE_URL`, `PLAYLIST_SERVICE_URL` | gateway/services | downstream base URLs |

## Project status

The monolith lives on `main`; the microservice split is developed on `feature/microservices`.
