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
  PostgreSQL 16  -- auth-db (:5434), user-db (:5435), music-db (:5436), playlist-db (:5437)
  Redis 7        -- cache, token revocation, rate limiting, OAuth flow state
  Kafka 3.9      -- topic simu.user.events (auth -> user, auth -> playlist)
  MinIO          -- local S3-compatible storage for audio files (:9000, console :9001)
  MailHog        -- local SMTP for verification/reset mails (:1025, UI :8025)
```

| Service | Owns | Public API | Internal API |
| --- | --- | --- | --- |
| `api-gateway` | – | everything behind `/api/v1/**` | – |
| `auth-service` | PostgreSQL `users`, `user_roles`, `outbox_events` | `/api/v1/auth/signup`, `/signin` | `GET/DELETE /api/v1/internal/users/**` |
| `user-service` | PostgreSQL `user_profiles`, `processed_events` | `/api/v1/users`, `/users/playlists`, `DELETE /users/me` | – |
| `playlist-service` | PostgreSQL `playlists`, `playlist_music`, `user_playlists` | `/api/v1/playlists/**` | `/api/v1/internal/users/{userId}/playlists` |
| `music-service` | PostgreSQL `music` + S3 bucket | `/api/v1/music/**` | `/api/v1/internal/music/{id}` |
| `common` | – (library) | UUID v7 generator, auditable base entity, Kafka event contracts | – |

### Why these pieces

**Local S3 with MinIO.** The music service speaks the S3 API only; `S3_ENDPOINT` decides where the
files really go. Locally that is MinIO: the bucket is created on start and made public-read, so the
browser can stream tracks straight from `http://localhost:9000/simu-music/...`. The SDK is configured
with `forcePathStyle=true`, which MinIO requires. Pointing `S3_ENDPOINT`/`S3_PUBLIC_URL` at a real
S3-compatible provider (Timeweb, AWS, Cloudflare R2) is a configuration-only change.

**PostgreSQL everywhere.** Every stateful service has its own PostgreSQL instance and schema:
`auth-db` (identity), `user-db` (profile read model), `music-db` (tracks), `playlist-db`
(playlists and library). No shared tables and no cross-database foreign keys - consistency between
services is kept by HTTP calls and Kafka events.

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

### Authentication and security

`auth-service` is the only place that knows credentials. The public surface:

| Endpoint | What it does |
| --- | --- |
| `POST /api/v1/auth/signup` | creates the account, sends the verification mail, returns an access + refresh pair |
| `POST /api/v1/auth/signin` | returns an access + refresh pair, writes a login-audit row |
| `POST /api/v1/auth/refresh` | rotates the pair; replay of a rotated token revokes the whole family |
| `POST /api/v1/auth/logout` | revokes the refresh token and blacklists the access token |
| `GET /api/v1/auth/me` | current profile and roles |
| `POST /api/v1/auth/verify-email` | confirms the address with a single-use token |
| `POST /api/v1/auth/forgot-password` | always 202, sends a reset link if the address exists |
| `POST /api/v1/auth/reset-password` | sets a new password and revokes every session |

How it works:

- **Access token** - JWT, 1 hour, claims `sub`, `roles`, `userId`, `jti`, `typ=access`, `iatMs`.
- **Refresh token** - opaque 256-bit value, stored only as a SHA-256 hash, 30 days, rotated on
  every refresh. Reusing an already rotated token is treated as theft: the whole token family is
  revoked.
- **Revocation** - logout writes `auth:revoked:{jti}` to Redis with the remaining TTL; account
  deletion writes `auth:user-blocked:{userId}`; a role change writes
  `auth:user-invalid-before:{userId}` (millisecond precision), so tokens issued before the change
  stop working while freshly issued ones keep the new roles. The gateway checks all three.
- **Internal API** - `/api/v1/internal/**` requires `X-Internal-Token`; the gateway strips that
  header from every inbound request, so a client cannot forge service-to-service calls.
- **Rate limiting** - the gateway keeps a Redis counter per IP and endpoint: 5 sign-ins, 5
  sign-ups, 3 recovery requests and 30 refreshes per minute.
- **Password policy** - enforced on the server (`@StrongPassword`): 6-100 characters, at least one
  uppercase letter and one digit.
- **E-mail** - verification and reset links go through SMTP; locally they land in MailHog
  (http://localhost:8025).
- **Audit** - every sign-in attempt (successful or not) is stored in `login_audit` with IP and
  user agent.

### Sign in with Google (Authorization Code Flow + PKCE)

```
GET  /api/v1/auth/oauth2/google            -> 302 to Google (state, nonce, PKCE S256, HttpOnly cookie)
GET  /api/v1/auth/oauth2/google/callback   -> validates, links the identity, 302 to the frontend with a ticket
POST /api/v1/auth/oauth2/exchange {ticket} -> {token, refreshToken, expiresIn, refreshExpiresIn}
```

What is verified, in this order:

1. the pending flow is read **and deleted** from Redis before any validation (`GETDEL`), so a
   replayed or raced callback finds nothing;
2. the `state` from Google is compared with the stored one in constant time, and the flow has a
   10-minute TTL; the flow id also lives in an `HttpOnly; SameSite=Lax` cookie, so a callback
   coming from another browser has no pending flow;
3. the code is exchanged server-side with `code_verifier` (PKCE S256) and the client secret in the
   Basic header;
4. the ID token is verified with the official `GoogleIdTokenVerifier` (signature against Google
   JWKS, audience, expiry, issuer) and the nonce is compared with the one stored for this flow;
5. the identity is matched by `(provider, subject)` - never by e-mail. An existing account is only
   linked when Google reports `email_verified=true`; otherwise the login is rejected (409). A new
   account is created without a password, so password login stays impossible for it;
6. the callback redirects to the frontend with a single-use ticket (60 s); tokens never appear in
   the URL, the browser history or the access log.

Google access/refresh tokens are not stored: they are needed only to prove identity once.

OAuth/2FA beyond Google are intentionally out of scope: a TOTP flow would add a second factor
without a product decision behind it, and other providers reuse the same `user_identities` table.

## Repository structure

- `SiMu-backend/` — Gradle multi-module backend
  - `common/` — UUID v7, `AuditableEntity`, Kafka event contracts
  - `api-gateway/`, `auth-service/`, `user-service/`, `music-service/`, `playlist-service/`
  - `docker-compose.yml`, `.env.example`
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
| auth database | `localhost:5434`, db `simu_auth` (user `simu`) |
| user database | `localhost:5435`, db `simu_user` |
| music database | `localhost:5436`, db `simu_music` |
| playlist database | `localhost:5437`, db `simu_playlist` |
| Redis | `localhost:6379` |
| Kafka (from the host) | `localhost:9094` |
| MailHog UI | http://localhost:8025 |
| Web client (Thymeleaf) | http://localhost:8090 |

Run a service outside Docker: `./gradlew :auth-service:bootRun` (needs the infrastructure containers
and the env variables below).

## Database migrations

Liquibase owns the schema of every service (`src/main/resources/db/changelog`); Hibernate runs in
`validate` mode, so the startup fails when an entity and the database drift apart. A new field means a
new changeset, not a silent `ddl-auto=update`.

```
db/changelog/
├── db.changelog-master.xml      # <include relativeToChangelogFile="true" .../>
└── changesets/
    ├── 001-init.xml
    └── 002-....xml
```

To inspect what has been applied:

```bash
docker compose exec auth-db psql -U simu -d simu_auth \
  -c "select id, filename, dateexecuted from databasechangelog order by orderexecuted"
# full schema reset in development
docker compose down -v && docker compose up -d --build
```

## Architecture deep-dive

[docs/architecture.md](docs/architecture.md) - services, databases and tables, keys and indexes,
Kafka events, Redis keys, security, request flows and failure modes.

![Architecture](docs/architecture-overview.png)
![Data model](docs/architecture-data.png)

## Environment variables

| Variable | Used by | Description |
| --- | --- | --- |
| `GATEWAY_PORT`, `AUTH_SERVICE_PORT`, `USER_SERVICE_PORT`, `MUSIC_SERVICE_PORT`, `PLAYLIST_SERVICE_PORT` | compose | published ports |
| `MUSIC_DB_*`, `PLAYLIST_DB_*` | music/playlist service | PostgreSQL connection for those services |
| `AUTH_DB_*` | auth-service | PostgreSQL connection (host, port, name, user, password) |
| `USER_DB_*` | user-service | PostgreSQL connection |
| `REDIS_HOST`, `REDIS_PORT` | auth/user service | cache |
| `KAFKA_BOOTSTRAP_SERVERS` | auth/user/playlist service | `kafka:9092` inside compose |
| `JWT_SECRET` | gateway + all services | HS256 secret |
| `JWT_ACCESS_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS` | auth-service | token lifetimes (1 h / 30 days) |
| `INTERNAL_API_KEY` | all services | service-to-service key for `/api/v1/internal/**` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM`, `APP_PUBLIC_URL` | auth-service | SMTP and the base URL used in e-mail links |
| `RATE_LIMIT_SIGNIN_PER_MINUTE`, `RATE_LIMIT_SIGNUP_PER_MINUTE`, `RATE_LIMIT_RECOVERY_PER_MINUTE`, `RATE_LIMIT_REFRESH_PER_MINUTE` | gateway | limits per IP |
| `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD` | MinIO | local object storage credentials |
| `S3_BUCKET`, `S3_REGION`, `S3_PUBLIC_URL` | music-service, MinIO | bucket and the URL the browser uses |
| `S3_ENDPOINT`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_PATH_STYLE` | music-service | any S3-compatible provider |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI` | auth-service | Google login; empty client id disables the endpoint (503) |
| `OAUTH_FRONTEND_REDIRECT_URI` | auth-service | where the callback sends the browser with the ticket |
| `OAUTH2_SECURE_COOKIE` | auth-service | set to true behind HTTPS |
| `CORS_ALLOWED_ORIGINS` | gateway | comma separated origins |
| `AUTH_SERVICE_URL`, `USER_SERVICE_URL`, `MUSIC_SERVICE_URL`, `PLAYLIST_SERVICE_URL` | gateway/services | downstream base URLs |

## Project status

The monolith lives on `main`; the microservice split is developed on `feature/microservices`.