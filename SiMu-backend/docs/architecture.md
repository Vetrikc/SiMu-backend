# SiMu — архитектура и устройство системы

Два документа-картинки: [architecture-overview.png](architecture-overview.png) — что с чем взаимодействует,
[architecture-data.png](architecture-data.png) — базы, таблицы, ключи и связи.

---

## 1. Что это

Музыкальный сервис: регистрация и вход (пароль или Google), загрузка треков в S3-совместимое хранилище,
плейлисты, библиотека пользователя, мягкое удаление.

Бэкенд — 4 независимых Spring Boot сервиса за одним API Gateway. У каждого сервиса **своя база PostgreSQL**
и **свои миграции Liquibase**. Взаимодействие: синхронные HTTP-вызовы через шлюз (клиент) или внутренние
эндпоинты (сервис-сервис) и асинхронные события Kafka.

Сверху работает серверный веб-клиент на Thymeleaf (порт 8090), который хранит токены в своей серверной
сессии и не отдаёт их в JS.

---

## 2. Сервисы

| Сервис | Порт | Стек | Ответственность | База |
|---|---|---|---|---|
| `api-gateway` | 8081 | Spring Cloud Gateway (WebFlux) | маршрутизация, проверка JWT, чёрный список, rate limit, CORS | — |
| `auth-service` | 8082 | Spring MVC, Security, JPA, Kafka, Redis, Mail, Google API | identity: users, роли, JWT, refresh-ротация, e-mail, OAuth, аудит | `simu_auth` |
| `user-service` | 8083 | Spring MVC, JPA, Kafka, Redis | профиль пользователя (проекция + кэш), fallback в auth-service | `simu_user` |
| `music-service` | 8084 | Spring MVC, JPA, AWS S3 SDK | метаданные треков, загрузка файлов | `simu_music` |
| `playlist-service` | 8085 | Spring MVC, JPA, Kafka | плейлисты, треки, библиотека пользователя (`/api/v1/me/playlists`) | `simu_playlist` |
| `web-client` | 8090 | Spring MVC + Thymeleaf | UI, серверная сессия с токенами | — |

Общий модуль `common` (библиотека, не сервис): `UuidV7`, `AuditableEntity` (created/updated/deleted),
контракты событий Kafka (`UserRegisteredEvent`, `UserDeletedEvent`, `RoleChangedEvent`, `KafkaTopics`).

---

## 3. Инфраструктура

| Компонент | Контейнер | Порт на хосте | Назначение |
|---|---|---|---|
| PostgreSQL auth | `simu-auth-db` | 5434 | база `simu_auth`, пользователь `simu` |
| PostgreSQL user | `simu-user-db` | 5435 | база `simu_user` |
| PostgreSQL music | `simu-music-db` | 5436 | база `simu_music` |
| PostgreSQL playlist | `simu-playlist-db` | 5437 | база `simu_playlist` |
| Redis | `simu-redis` | 6379 | кэш, чёрный список токенов, rate limit, OAuth-flow, тикеты |
| Kafka | `simu-kafka` | 9092 (внешний 9094) | события жизненного цикла пользователя |
| MinIO | `simu-minio` | 9000 / 9001 (консоль) | S3-совместимое хранилище аудио |
| MailHog | `simu-mailhog` | 1025 (SMTP) / 8025 (UI) | письма подтверждения и сброса пароля |

Все контейнеры в сети `simu_default`; сервисы обращаются друг к другу по именам (`auth-db`, `kafka`, ...).

---

## 4. Базы данных

### 4.1 Принципы

- **одна база на сервис**, никаких общих таблиц и кросс-базовых FK;
- схема создаётся **Liquibase**, Hibernate работает в режиме `validate` (старт падает, если entity и БД расходятся);
- у всех бизнес-сущностей `created_at`, `updated_at`, `deleted_at` (`AuditableEntity`);
- удаление — **мягкое**: строка остаётся, `@SQLRestriction("deleted_at is null")` скрывает её из всех запросов;
- уникальность среди «живых» строк — **частичные индексы** `WHERE deleted_at IS NULL`;
- идентификаторы: `BIGINT IDENTITY` для доменных сущностей (контракт фронта), **UUID v7** для технических
  таблиц (события, токены, аудит, проекция) — время-упорядоченный, индекс не фрагментируется.

### 4.2 `simu_auth` (auth-service)

| Таблица | Назначение | Ключи и индексы |
|---|---|---|
| `users` | аккаунт: логин, e-mail, пароль (BCrypt), роли, флаг e-mail | PK `id`; частичные UK `username`, `email`; `ix_users_deleted_at` |
| `user_roles` | роли пользователя (`USER`, `ADMIN`) | PK (`user_id`, `role`); FK → `users.id` CASCADE |
| `refresh_tokens` | выданные refresh-токены (хранится SHA-256) | PK `id`; UK `token_hash`; `family_id` (ротация), `replaced_by`, `revoked_at`; индексы по user и family |
| `email_tokens` | одноразовые токены verify/reset | PK `id`; UK `token_hash`; `type`, `expires_at`, `used_at` |
| `login_audit` | журнал входов | PK `id`; индексы (`user_id`, `created_at DESC`) и (`created_at DESC`); `auth_method`, `subject_hash` |
| `user_identities` | внешние identity (Google) | PK `id`; **UQ (`provider`, `subject`)**; FK → users |
| `outbox_events` | transactional outbox для Kafka | PK `id` (UUID v7); частичный индекс `WHERE published_at IS NULL` |

### 4.3 `simu_user` (user-service)

| Таблица | Назначение | Ключи |
|---|---|---|
| `user_profiles` | read-model профиля, наполняется из Kafka | PK `id`; частичные UK `user_id`, `username`; `roles` |
| `processed_events` | идемпотентность консьюмера | PK `event_id` |

### 4.4 `simu_music` (music-service)

| Таблица | Назначение | Ключи |
|---|---|---|
| `music` | метаданные трека (`name`, `artist`, `url`) | PK `id`; индексы `artist`, `deleted_at` |

Файл лежит не в БД: `url` собирается как `S3_PUBLIC_URL + "/" + bucket + "/" + key`.

### 4.5 `simu_playlist` (playlist-service)

| Таблица | Назначение | Ключи |
|---|---|---|
| `playlists` | плейлист (`name`, `description`, `image_url`) | PK `id`; индекс `deleted_at` |
| `playlist_music` | упорядоченные треки плейлиста | PK (`playlist_id`, `position`); FK → `playlists.id` CASCADE |
| `user_playlists` | библиотека пользователя | PK `id`; UQ (`user_id`, `playlist_id`); FK → `playlists.id`; индексы `user_id`, `deleted_at` |

### 4.6 Логические связи между базами (без FK)

| Ссылка | На что указывает | Кто следит за целостностью |
|---|---|---|
| `simu_user.user_profiles.user_id` | `simu_auth.users.id` | Kafka-события (`UserRegistered`, `RoleChanged`, `UserDeleted`) |
| `simu_playlist.user_playlists.user_id` | `simu_auth.users.id` | консьюмер `UserDeleted` в playlist-service |
| `simu_playlist.playlist_music.music_id` | `simu_music.music.id` | HTTP-проверка `GET /api/v1/internal/music/{id}` перед добавлением |
| `simu_auth.outbox_events.aggregate_id` | `simu_auth.users.id` | пишется в одной транзакции с изменением пользователя |

---

## 5. Liquibase

Миграции лежат в `src/main/resources/db/changelog` каждого сервиса:

```
db/changelog/
├── db.changelog-master.xml          # список include
└── changesets/
    ├── 001-init.xml                 # auth: users, roles, outbox
    ├── 002-auth-hardening.xml       # auth: refresh/email токены, аудит
    └── 003-oauth-identities.xml     # auth: внешние identity
```

- подключение: `spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml`;
- `include` идёт с `relativeToChangelogFile="true"`;
- служебные таблицы: `databasechangelog` (применённые changeset) и `databasechangeloglock`;
- частичные индексы и `DESC`-индексы описаны тегом `<sql>`, остальное — нативные `createTable/addColumn/createIndex`;
- Hibernate: `ddl-auto=validate` — схема полностью принадлежит Liquibase.

**Как добавить миграцию:** создать `changesets/004-xxx.xml`, добавить `<include>` в master,
перезапустить сервис. Старые БД догонят только новый changeset, новые — применят всё с нуля.

Полезные команды:

```bash
# что применено
docker compose exec auth-db psql -U simu -d simu_auth \
  -c "select id, author, filename, dateexecuted from databasechangelog order by orderexecuted"
# полный сброс схемы (dev): удалить тома и поднять заново
docker compose down -v && docker compose up -d --build
```

---

## 6. Kafka

- топик `simu.user.events`, 1 партиция, KRaft-режим;
- событие несёт заголовок `eventType` и JSON-payload:

| Событие | Когда | Payload | Кто читает |
|---|---|---|---|
| `UserRegistered` | регистрация (пароль или Google) | eventId, occurredAt, userId, username, email | user-service |
| `RoleChanged` | смена ролей | eventId, occurredAt, userId, roles | user-service |
| `UserDeleted` | удаление аккаунта | eventId, occurredAt, userId | user-service, playlist-service |

- **Transactional outbox**: `auth-service` пишет событие в `outbox_events` **в той же транзакции**, что и
  изменение пользователя. Планировщик (`OutboxPublisher`, раз в секунду) публикует неопубликованные строки
  и проставляет `published_at`; ошибки остаются в таблице с `attempts`/`last_error` и повторяются.
- Консьюмеры идемпотентны: `processed_events` по `eventId`, повторная доставка — no-op.
- Группы: `user-service`, `playlist-service` — каждый получает все события независимо.

---

## 7. Redis

| Ключ | TTL | Кто пишет / читает | Зачем |
|---|---|---|---|
| `simu:auth:users::{username}` | 10 мин | auth / auth | кэш identity для внутренних вызовов |
| `simu:user:profiles::{username}` | 10 мин | user / user | кэш профиля |
| `auth:revoked:{jti}` | до exp токена | auth (logout) / gateway | отзыв одного access-токена |
| `auth:user-blocked:{userId}` | 1 ч | auth (удаление) / gateway | отзыв всех токенов пользователя |
| `auth:user-invalid-before:{userId}` | 1 ч | auth (смена ролей) / gateway | токены, выпущенные до метки, невалидны |
| `rl:{path}:{ip}:{minute}` | 2 мин | gateway | счётчик rate limit |
| `oauth:flow:{flowId}` | 10 мин | auth | state/nonce/verifier незавершённого Google-входа |
| `oauth:ticket:{ticket}` | 60 с | auth | одноразовый тикет для обмена на токены |

---

## 8. Безопасность

- **Access-токен**: JWT HS256, 1 час, claims `sub`, `roles`, `userId`, `jti`, `typ=access`, `iatMs`.
- **Refresh-токен**: 256 бит, в БД только SHA-256, 30 дней, ротация на каждом использовании.
  Повторное использование → отзыв всей семьи (`family_id`) и 401.
- **Logout**: refresh гасится в БД, `jti` access попадает в Redis до истечения.
- **Шлюз** проверяет подпись, `exp`, `typ`, чёрные списки и только потом пробрасывает
  `X-User-Id`, `X-User-Name`, `X-User-Roles`; клиентские `X-User-*` и `X-Internal-Token` вырезаются.
- **Внутренние эндпоинты** (`/api/v1/internal/**`) не маршрутизируются шлюзом и требуют `X-Internal-Token`.
- **Rate limit** на шлюзе по IP: signin/signup 5/мин, recovery 3/мин, refresh 30/мин, oauth 10/мин (дефолты).
- **Пароль**: 6–100 символов, минимум одна заглавная и одна цифра — проверяется на сервере.
- **E-mail**: подтверждение и сброс пароля одноразовыми токенами; в dev письма — MailHog.
- **Google OAuth**: Authorization Code + PKCE (S256), state/nonce/verifier в Redis и HttpOnly-cookie,
  проверка подписи ID-токена по JWKS, `aud`, `iss`, `exp`, `nonce`; линковка только по
  `(provider, subject)` и только при `email_verified=true`.

---

## 9. API

### Публичное (через шлюз :8081)

| Метод и путь | Сервис | Назначение |
|---|---|---|
| `POST /api/v1/auth/signup` | auth | регистрация, access+refresh |
| `POST /api/v1/auth/signin` | auth | вход, access+refresh |
| `POST /api/v1/auth/refresh` | auth | ротация пары |
| `POST /api/v1/auth/logout` | auth | отзыв refresh + чёрный список access |
| `GET /api/v1/auth/me` | auth | профиль и роли |
| `POST /api/v1/auth/verify-email` | auth | подтверждение e-mail |
| `POST /api/v1/auth/forgot-password` | auth | запрос письма (всегда 202) |
| `POST /api/v1/auth/reset-password` | auth | новый пароль, отзыв сессий |
| `GET /api/v1/auth/oauth2/google` + `/callback` + `POST /exchange` | auth | вход через Google |
| `GET /api/v1/users` | user | профиль |
| `GET/POST /api/v1/me/playlists` | playlist | библиотека текущего пользователя (userId из `X-User-Id`) |
| `DELETE /api/v1/users/me` | user | удаление аккаунта |
| `GET /api/v1/music`, `GET /api/v1/music/{id}` | music | список и трек |
| `POST /api/v1/music` | music | загрузка (multipart) |
| `DELETE /api/v1/music/{id}` | music | мягкое удаление |
| `GET /api/v1/playlists/{id}` | playlist | плейлист (только свой, чужой id → 404) |
| `POST /api/v1/playlists/{id}/tracks/{musicId}` | playlist | добавить трек (только в свой) |
| `DELETE /api/v1/playlists/{id}` | playlist | мягкое удаление (только своего) |

### Проверка владения

Плейлист доступен только через **живую ссылку в библиотеке вызывающего**:

```java
private void requireOwned(Long playlistId, Long userId) {
    if (!userPlaylistRepository.existsByUserIdAndPlaylistId(userId, playlistId)) {
        throw new PlaylistNotFoundException("Playlist not found with ID: " + playlistId);   // 404
    }
}
```

- `userId` берётся из `X-User-Id` (его ставит шлюз после проверки JWT), а не из URL или тела;
- проверка стоит первой строкой в каждом методе по id (чтение, добавление трека, удаление);
- чужой id отвечает **404**, а не 403: нельзя проверить, существует ли плейлист;
- мягко удалённые ссылки не учитываются (`@SQLRestriction("deleted_at is null")`), поэтому удалённый плейлист так же невидим.

### Внутреннее (без шлюза, `X-Internal-Token`)

| Метод и путь | Кто вызывает | Зачем |
|---|---|---|
| `GET /api/v1/internal/users/{username}` | user | профиль при промахе проекции |
| `DELETE /api/v1/internal/users/{id}` | user | удаление аккаунта |
| `PATCH /api/v1/internal/users/{id}/roles` | admin/ops | смена ролей |
| `POST /api/v1/internal/users/{userId}/default-playlist` | auth | плейлист «Favorites» при регистрации |
| `GET /api/v1/internal/music/{id}` | playlist | проверка существования трека |

---

## 10. Ключевые сценарии

### 10.1 Регистрация

1. `POST /auth/signup` → шлюз (публичный путь) → auth.
2. auth: проверка уникальности, BCrypt-хеш, `users` + `user_roles`, `email_tokens` (verify) и
   `outbox_events(UserRegistered)` — **одна транзакция**.
3. Письмо через SMTP (MailHog в dev).
4. Внутренний вызов auth → playlist: создать «Favorites» (не критично для успеха регистрации).
5. Ответ: access + refresh; `OutboxPublisher` публикует событие → user-service создаёт `user_profiles`.

### 10.2 Вход и запросы

1. `POST /auth/signin` → проверка пароля → refresh-строка (хэш в БД) + JWT.
2. Клиент (Thymeleaf) кладёт пару в серверную сессию; браузер получает только `JSESSIONID`.
3. Каждый запрос: клиент → шлюз с `Authorization: Bearer` → проверка подписи/exp/typ/чёрного списка →
   сервис с `X-User-*`.
4. Если access истёк — клиент сам вызывает `/auth/refresh` и повторяет запрос.

### 10.3 Обновление токена

`POST /auth/refresh` → поиск по SHA-256 → отозван/просрочен → 401; иначе старый помечается `revoked`,
новый в той же `family_id`, `replaced_by` заполняется. Повторное использование старого = кража → отзыв семьи.

### 10.4 Загрузка трека

`POST /music` (multipart: `file` + JSON `music`) → music: файл в MinIO через S3 SDK (path-style) →
строка `music` с публичным URL. При ошибке S3 строка не создаётся.

### 10.5 Добавление трека в плейлист

Клиент → playlist: `POST /playlists/{id}/tracks/{musicId}` → playlist проверяет трек через
`GET /internal/music/{id}` → вставляет строку в `playlist_music` с `position = size`.

### 10.6 Удаление аккаунта

`DELETE /users/me` → user → auth `DELETE /internal/users/{id}`: soft delete, outbox `UserDeleted`,
отзыв refresh и `auth:user-blocked:{id}` (токены сразу невалидны) → user гасит свою проекцию →
playlist по событию гасит `user_playlists` и плейлисты пользователя.

---

## 11. Отказоустойчивость и особенности

- Все сервисы **stateless** (кроме серверной сессии веб-клиента) — можно запускать несколько реплик за шлюзом.
- Kafka недоступна: auth продолжает работать, события копятся в `outbox_events` и уходят после восстановления.
- Redis недоступен: шлюз отвечает 5xx на защищённые маршруты (fail-closed), кэш просто перестаёт работать.
- PostgreSQL недоступен: сервис не стартует (Liquibase + validate) или отдаёт 5xx; health `DOWN`.
- MinIO недоступен: загрузка трека падает, метаданные не пишутся; чтение библиотеки работает.
- Часовые пояса: все метки времени — `timestamptz` (UTC).

---

## 12. Переменные окружения

Основные (полный список — в `.env.example`):

| Переменная | Где | Смысл |
|---|---|---|
| `GATEWAY_PORT`, `AUTH_SERVICE_PORT`, `USER_SERVICE_PORT`, `MUSIC_SERVICE_PORT`, `PLAYLIST_SERVICE_PORT` | compose | публикуемые порты |
| `AUTH_DB_*`, `USER_DB_*`, `MUSIC_DB_*`, `PLAYLIST_DB_*` | сервисы | host/port/name/user/password каждой БД |
| `JWT_SECRET`, `JWT_ACCESS_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS` | auth, gateway | подпись и время жизни токенов |
| `INTERNAL_API_KEY` | все сервисы | ключ внутренних эндпоинтов |
| `REDIS_HOST`, `REDIS_PORT` | auth, user, gateway | кэш и отзыв токенов |
| `KAFKA_BOOTSTRAP_SERVERS` | auth, user, playlist | брокер |
| `S3_ENDPOINT`, `S3_PUBLIC_URL`, `S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY` | music | хранилище |
| `MAIL_HOST`, `MAIL_PORT`, `APP_PUBLIC_URL` | auth | письма и ссылки |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI` | auth | Google OAuth |
| `OAUTH_FRONTEND_REDIRECT_URI` | auth | куда вернуть браузер после Google |
| `RATE_LIMIT_*` | gateway | лимиты по IP |
| `CORS_ALLOWED_ORIGINS` | gateway | разрешённые origin |

---

## 13. Диагностика

```bash
docker compose ps
docker compose logs -f auth-service
docker exec simu-auth-db psql -U simu -d simu_auth -c "select count(*) from users"
docker exec simu-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
docker exec simu-redis redis-cli --scan --pattern 'auth:*'
curl -s localhost:8081/actuator/health
```