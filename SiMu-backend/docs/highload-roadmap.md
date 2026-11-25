# План работ: SiMu → enterprise-реализация под высокую нагрузку

Документ описывает **что нужно сделать**, какими средствами и **что мешает** это сделать сейчас.
Реализация не выполнялась.

Целевые показатели: **8 000 RPS** на одной машине (Ubuntu, 6 ядер / 12 потоков), p99 < 150 мс,
полная бизнес-логика сохраняется; хранение и отдача аудио — вне машины (S3/CDN).

---

## Статус реализации

| Группа | Статус | Что сделано |
|---|---|---|
| A. API и данные | ✅ | keyset-пагинация `/music?limit&cursor`, поиск `/music/search`, Redis-кэш страниц/поиска/библиотеки, pg_trgm-индексы |
| B. Модель исполнения | ✅ | virtual threads, Tomcat 400, Hikari 20, таймауты RestClient 300 мс/1 с, graceful shutdown |
| C. Надёжность | ✅ | outbox `FOR UPDATE SKIP LOCKED` + батч + gauge, 6 партиций, DLT + DefaultErrorHandler, idempotency-key |
| D. Наблюдаемость | ✅ (частично) | Micrometer/Prometheus, health-группы, логи без INFO в контроллерах; OTel-экспорт включается флагом |
| E. Платформа | 🟡 | `deploy/k8s/base` (Deployment/Service/Ingress/HPA/PDB/NetworkPolicy) + README; операторы и Job для миграций — следующие шаги |
| F. Процессы | 🟡 | GitHub Actions: сборка, тесты, образы в GHCR, валидация kustomize; Testcontainers-тестов пока нет |

---

## 0. Сводка по группам

| Группа | Проблемы | Эффект | Сложность |
|---|---|---|---|
| A. API и данные | нет пагинации, нет поиска, нет кэша чтения | +RPS, −CPU, −размер ответа | средняя |
| B. Модель исполнения | нет виртуальных потоков, Tomcat 200, нет таймаутов, нет graceful shutdown | −задержка, предсказуемость | низкая |
| C. Надёжность | outbox не под реплики, Kafka 1 партиция/concurrency=1/нет DLQ, нет идемпотентности POST | консистентность, отсутствие дублей | средняя |
| D. Наблюдаемость | нет метрик, JSON-логов, traceId; INFO на каждый запрос | видеть узкие места | низкая |
| E. Платформа | только docker-compose, нет k8s, проб, HPA, секретов | масштабирование, деплой | высокая |
| F. Процессы | нет CI/CD, мало тестов, нет SLO | скорость и качество изменений | средняя |

Порядок внедрения: **D → B → A → C → E → F** (сначала видимость, затем быстрые победы, затем платформа).

---

## A. API и данные

### A1. Пагинация списка треков

**Что не так.** `MusicService.getAllMusic()` вызывает `findAll()` и отдаёт всю таблицу;
размер ответа и время сериализации растут вместе с базой.

**Шаги.**
1. Ввести контракт `GET /api/v1/music?limit=50&cursor=<id>` (keyset-пагинация по `id`).
2. Обёртка ответа: `{ "items": [...], "nextCursor": 123 }` — вместо голого массива.
3. Дефолтный лимит 50, максимум 200; при отсутствии параметров поведение совместимо (первая страница).
4. Индекс под сортировку: `(deleted_at, id)`; для поиска — `(artist, id)`.
5. Аналогично для библиотеки: `GET /api/v1/me/playlists?limit=20&cursor=`.
6. Обновить клиенты (Thymeleaf, React-схема) под новый формат.

**Стек.** Spring Data `Pageable`/`Slice`, либо кастомный keyset-запрос через `@Query`;
Liquibase-миграция для индексов.

**Что блокирует.**
- Ломается контракт: текущие клиенты ждут массив → нужен переходный период или версия `/api/v2`.
- Нет индексов под сортировку — без миграции пагинация будет медленной.
- Тесты отсутствуют, регресс контракта нечем поймать.

**Готово, когда.** `EXPLAIN ANALYZE` показывает Index Scan, ответ ≤ 2 КБ, p99 на списке < 20 мс.

### A2. Кэш чтения

**Что не так.** Кэшируются только профиль и identity; `GET /music` и `GET /me/playlists`
идут в БД на каждый запрос.

**Шаги.**
1. `@Cacheable` на «первую страницу» списка треков (ключ: `limit, cursor`), TTL 30–60 с.
2. `@Cacheable` на библиотеку (ключ: `userId`), TTL 30 с.
3. Инвалидация: `@CacheEvict(allEntries)` при создании/удалении трека; точечная — при изменении плейлиста.
4. Двухуровневый кэш: Caffeine (L1, 5–10 с) + Redis (L2, 30–60 с) — снимает сетевой хоп с горячих ключей.
5. Метрики попаданий: `CacheMetricsRegistrar` + `cache_gets_total`.

**Стек.** Spring Cache, Caffeine, Redis; Micrometer cache metrics.

**Что блокирует.**
- Нет событий об изменении медиатеки между сервисами — инвалидация будет только локальной.
- Один Redis на всё; при 8k RPS и 2–3 ключах на запрос он станет третьим узким местом (нужен отдельный инстанс под кэш).
- Нет TTL-политики и метрик — легко получить «вечно устаревший» кэш.

### A3. Поиск по медиатеке

**Что не так.** Есть только полный список; фильтров по названию/исполнителю нет.

**Шаги.**
1. `GET /api/v1/music/search?q=&artist=&limit=` с keyset-пагинацией.
2. Индекс `pg_trgm` GIN на `name` и `artist`; запрос `ILIKE` или `websearch_to_tsquery`.
3. Кэш результатов поиска (TTL 60 с), ключ — нормализованный запрос.
4. Ограничение длины запроса и защита от «мусорных» запросов (минимум 2 символа).

**Стек.** PostgreSQL `pg_trgm`/`tsvector`, Spring Data `@Query`, Redis-кэш.

**Что блокирует.** Сначала нужна A1 (иначе поиск снова вернёт всё); нет индексов; нет тестовых данных объёма.

---

## B. Модель исполнения

### B1. Виртуальные потоки и потоки Tomcat

**Что не так.** Java 21 есть, но `spring.threads.virtual.enabled` не включён; Tomcat на дефолтных
200 потоках. При 8k RPS и блокирующих вызовах БД потоки кончаются раньше CPU.

**Шаги.**
1. `spring.threads.virtual.enabled=true` во всех сервисах.
2. `server.tomcat.threads.max=400`, `accept-count=200`, `max-connections=10000`.
3. Проверить pinning: `-Djdk.tracePinnedThreads=full` под нагрузкой (synchronized-блоки, JDBC).
4. Убедиться, что Hikari не стал узким: пул 30–50 + PgBouncer (transaction pooling, `prepareThreshold=0`).

**Стек.** Spring Boot 3.4 virtual threads, Tomcat, HikariCP, PgBouncer.

**Что блокирует.**
- PgBouncer отсутствует в compose — без него рост числа потоков упрётся в 10 соединений.
- Kafka-консьюмеры не используют виртуальные потоки автоматически (нужен `concurrency`).

### B2. Таймауты и graceful shutdown

**Что не так.** `RestClient` в `AuthServiceClient`, `MusicServiceClient`, `PlaylistClient` создан без
таймаутов; при зависании соседа потоки висят бесконечно. Нет `server.shutdown=graceful`.

**Шаги.**
1. Request factory с connect 300 мс / read 1 с (JDK HttpClient).
2. Resilience4j: Retry (идемпотентные GET), CircuitBreaker, Bulkhead, TimeLimiter.
3. `server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase=20s`.
4. Метрики таймаутов и разомкнутых цепей.

**Стек.** `JdkClientHttpRequestFactory`, Resilience4j + micrometer, Spring Boot graceful shutdown.

**Что блокирует.** Нет зависимостей Resilience4j и метрик; нет тестов на деградацию соседа.

---

## C. Надёжность и консистентность

### C1. Outbox под несколькими репликами

**Что не так.** `OutboxPublisher` читает `findTop100` без блокировки строк и синхронно ждёт
`.get(5, TimeUnit.SECONDS)` на каждое событие внутри `@Transactional`. Две реплики auth
опубликуют одно событие дважды; транзакция держится на время сети.

**Шаги.**
1. `SELECT ... FOR UPDATE SKIP LOCKED LIMIT 100` — каждая реплика берёт свои строки.
2. Публикация вне транзакции БД: сначала зафиксировать «взятие», затем отправлять.
3. Батч-отправка (`kafkaTemplate.send` с `CompletableFuture.allOf`), обновление `published_at` пачкой.
4. Метрика лага: `outbox_unpublished_count`, `outbox_oldest_age_seconds`.
5. Альтернатива enterprise-уровня: Debezium CDC (чтение WAL, без планировщика).

**Стек.** PostgreSQL `SKIP LOCKED`, Spring Kafka, Micrometer; опционально Debezium + Kafka Connect.

**Что блокирует.** Нет Kafka Connect/Debezium в инфраструктуре; тесты идемпотентности отсутствуют.

### C2. Kafka: партиции, concurrency, DLQ

**Что не так.** Топик `simu.user.events` создаётся с 1 партицией, консьюмеры с `concurrency=1`,
нет обработчика ошибок и Dead Letter Topic.

**Шаги.**
1. Топик на 6 партиций, ключ — `userId` (порядок в рамках пользователя сохраняется).
2. `spring.kafka.listener.concurrency=3`.
3. `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` → топик `simu.user.events.DLT`.
4. Экспоненциальный backoff, лимит попыток, метрики ретраев и DLT.
5. Явное создание топиков через `NewTopic`-бины (не полагаться на auto-create).

**Стек.** Spring Kafka, KafkaAdmin/NewTopic, DeadLetterPublishingRecoverer, Micrometer.

**Что блокирует.**
- Увеличение партиций существующего топика меняет маппинг ключей — надёжнее пересоздать топик.
- Нет мониторинга лага консьюмеров.

### C3. Идемпотентность POST

**Что не так.** Повторный запрос (retry клиента или сети) создаёт второй плейлист/трек.

**Шаги.**
1. Заголовок `Idempotency-Key` на POST.
2. Таблица `idempotency_keys(key, user_id, method, path, request_hash, response_status, response_body, created_at)`.
3. Фильтр/интерцептор: нет ключа → выполнить и сохранить; есть → вернуть сохранённый ответ (или 409 при другом теле).
4. TTL 24 часа, очистка джобой.
5. Плюс `@Version` на `PlayList` — защита от конкурентных `addTrack`.

**Стек.** Liquibase-миграция, Spring `OncePerRequestFilter`, PostgreSQL.

**Что блокирует.** Нет таблицы и миграции; ключ надо хранить в БД сервиса, а не в шлюзе (иначе общий стор).

---

## D. Наблюдаемость

### D1. Метрики

**Что не так.** Actuator отдаёт только `health` и `info`; метрик нет, насыщение видно только по `docker stats`.

**Шаги.**
1. Зависимость `micrometer-registry-prometheus`, экспозиция `/actuator/prometheus`.
2. Метрики: HTTP (RPS, p95/p99 через гистограммы), Hikari (active/idle/pending), Tomcat (busy threads),
   Kafka (consumer lag), Redis (latency), кэш (hit ratio), outbox (лаг).
3. Prometheus + Grafana + Alertmanager; в k8s — kube-prometheus-stack.
4. Алерты: p99 > 300 мс, error rate > 1 %, pending Hikari > 0, лаг outbox > 60 с.

**Стек.** Micrometer, Prometheus, Grafana, Alertmanager.

**Что блокирует.** Нет зависимостей и конфигурации экспозиции; нет стенда мониторинга.

### D2. Логи и трассировка

**Что не так.** INFO на каждый запрос в контроллерах; логи текстовые, без `traceId`; корреляция между сервисами невозможна.

**Шаги.**
1. Убрать `log.info` из контроллеров, оставить DEBUG; access-логи — на шлюзе.
2. Structured logging: `logging.structured.format.console=ecs` (Spring Boot 3.4).
3. Micrometer Tracing + OTel bridge, экспорт в Jaeger/Tempo; `traceId`/`spanId` в MDC автоматически.
4. Сбор логов: Loki (или ELK), запросы по `traceId`.

**Стек.** Boot structured logging, `micrometer-tracing-bridge-otel`, OTLP exporter, Grafana Tempo/Loki.

**Что блокирует.** Нет коллектора; нет метрик, к которым трассировка привязывается.

---

## E. Платформа: Kubernetes

**Что не так.** Развёртывание — `docker-compose` с опубликованными портами, паролями из `.env` и
миграциями при старте приложения. Горизонтальное масштабирование и rolling update не поддержаны.

**Шаги.**
1. **Локальный кластер**: k3s на Ubuntu (или kind/minikube для разработки).
2. **Объекты**: Namespace, Deployment (5 приложений), Service (ClusterIP), Ingress (nginx) + TLS (cert-manager),
   ConfigMap, Secret (sealed-secrets), HPA (CPU 60 % + RPS через Prometheus Adapter), PodDisruptionBudget.
3. **Пробы**: `management.endpoint.health.probes.enabled=true`, группы `liveness`/`readiness`/`startup`;
   liveness не должна зависеть от БД, readiness — зависит.
4. **Ресурсы**: requests/limits (gateway 500m/512Mi, сервисы 500m/768Mi), JVM `-XX:MaxRAMPercentage=75`.
5. **Миграции**: Liquibase отдельным Job/initContainer, приложение — `ddl-auto=validate`.
6. **Stateful-компоненты**: CloudNativePG (Postgres), Strimzi (Kafka), Redis operator, MinIO operator —
   либо managed-сервисы (проще для 6-ядерной машины).
7. **Сеть**: NetworkPolicy (только шлюз слушает Ingress), внутренние вызовы по Service DNS.
8. **CI**: сборка образов → registry (GHCR), тег по git SHA, деплой Helm-чартом.

**Стек.** Kubernetes (k3s/kind), Helm или Kustomize, ingress-nginx, cert-manager, sealed-secrets,
HPA + Prometheus Adapter, CloudNativePG, Strimzi, Grafana Loki/Tempo, GHCR, GitHub Actions.

**Что блокирует.**
- **Ресурсы машины**: 6 ядер / 8 ГБ под Docker недостаточно для k8s + 4 PostgreSQL + Kafka + Redis + MinIO + 5 сервисов.
  Варианты: внешние managed-сервисы, один инстанс PG с несколькими БД (нарушает текущую модель),
  либо k3s на «голом» Ubuntu с тюнингом.
- **Нет registry и CI** — образы собираются локально.
- **Секреты в `.env`** — нужен sealed-secrets/Vault.
- **Миграции при старте** — гонка при rollout нескольких реплик.
- **Health-пробы** сейчас включают DB в общий health — liveness будет «убивать» поды при недоступности БД.
- **Windows-машина разработчика**: kind на Docker Desktop удваивает накладные расходы.

**Готово, когда.** `kubectl rollout` без простоя, HPA масштабирует по RPS, секреты не в git, миграции — Job.

---

## F. Процессы

| Проблема | Шаги | Что блокирует |
|---|---|---|
| Нет CI | GitHub Actions: сборка, тесты, образы, пуш в GHCR, деплой Helm | Нет workflow-файлов и registry |
| Мало тестов | Testcontainers (Postgres, Kafka, Redis), контрактные тесты | H2 в тестах не покрывает реальное поведение |
| Нет SLO | SLO на p99, error rate, лаг outbox; алерты | Нет метрик (D1) |

---

## Порядок внедрения (фазы)

| Фаза | Работы | Зачем | Оценка |
|---|---|---|---|
| 0 | D1, D2, B2 (метрики, логи, таймауты, graceful) | без видимости остальное — вслепую | 2–3 дня |
| 1 | A1, A2, A3 (пагинация, кэш, поиск) | главный прирост RPS | 3–4 дня |
| 2 | B1, C1, C2, C3 (виртуальные потоки, outbox, Kafka, идемпотентность) | устойчивость под 8k | 4–5 дней |
| 3 | E (k8s, Helm, пробы, HPA, секреты, CI) | масштабирование и деплой | 1–2 недели |
| 4 | Нагрузочный тест 8k, SLO, алерты, документация | подтверждение цели | 2–3 дня |

---

## Что блокирует нас сильнее всего

1. **Нет наблюдаемости** — нельзя ни измерить, ни доказать эффект изменений.
2. **Нет CI** — каждое изменение проверяется вручную, регресс ловится поздно.
3. **Ресурсы одной машины** — k8s со всем stateful-стеком на 6 ядрах нереалистичен; нужны внешние БД/брокер или отдельный хост.
4. **Контракт API без пагинации** — менять его придётся синхронно с клиентами.
5. **Миграции в приложении** — при переходе на k8s это гонка при rollout; нужен отдельный Job.

---

## Критерии приёмки (DoD)

- [ ] `GET /api/v1/music` отдаёт ≤ 50 элементов, p99 < 20 мс, ответ ≤ 2 КБ.
- [ ] При 8 000 RPS: p99 < 150 мс, error rate < 0,1 %, Hikari pending = 0.
- [ ] Метрики в Prometheus, дашборд Grafana, алерты на p99/error rate/лаг outbox.
- [ ] Убийство пода/реплики не теряет события и не рвёт запросы (graceful shutdown).
- [ ] Повтор POST с тем же `Idempotency-Key` не создаёт дубль.
- [ ] Kafka: 6 партиций, DLT, lag < 1 с в норме.
- [ ] `helm upgrade` без простоя, секреты вне git, миграции — Job.