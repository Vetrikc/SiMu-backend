# DevOps: запуск, мониторинг, Kubernetes

## 1. Локальный запуск (Docker Compose)

```bash
cp .env.example .env      # заполнить секреты
docker compose up -d --build
```

| Адрес | Что |
|---|---|
| http://localhost:8081 | edge (nginx) — единственная точка входа в API |
| http://localhost:8090 | демо-клиент (отдельный процесс, `SiMu-frontend`) |
| http://localhost:9090 | Prometheus |
| http://localhost:3000 | Grafana (admin / `GRAFANA_PASSWORD`, по умолчанию admin) |
| http://localhost:16686 | Jaeger UI |
| http://localhost:8025 | MailHog (письма подтверждения) |
| http://localhost:9001 | MinIO console |

Топология запроса:

```
клиент :8090
  └─► edge :8081
        ├─ /api/v1/music/**  ──► music-service, music-service-2
        └─ /**               ──► api-gateway, api-gateway-2, api-gateway-3
                                   ├─ auth-service
                                   ├─ user-service
                                   └─ playlist-service
```

## 2. Метрики

Каждый сервис отдаёт `/actuator/prometheus`. Prometheus собирает их через Docker DNS
(`dns_sd_configs`), поэтому реплики подхватываются автоматически, без правки конфига.

Дашборд **SiMu overview** провижинится в Grafana: RPS по сервисам, p95/p99, 5xx, Hikari
(active/pending), heap JVM, попадания в кэш, занятые потоки Tomcat.

Полезные запросы:

```promql
sum by (job) (rate(http_server_requests_seconds_count[1m]))
histogram_quantile(0.99, sum by (job, le) (rate(http_server_requests_seconds_bucket[1m])))
sum by (job) (hikaricp_connections_pending)
sum by (job) (rate(cache_gets_total{result="hit"}[1m]))
```

## 3. Трейсинг

Включён через `deploy/monitoring/tracing.env` (`TRACING_ENABLED=true`, семплирование 10 %).
Экспортёр — OTLP HTTP в Jaeger. Чтобы выключить, уберите `env_file` у сервиса или
поставьте `TRACING_ENABLED=false`.

## 4. Kubernetes локально (kind)

```bash
bash scripts/k8s/up.sh            # кластер + образы + манифесты
kubectl -n simu get pods
curl http://localhost:8081/actuator/health
```

Что делает скрипт:

1. создаёт kind-кластер `simu` (1 control-plane + 1 worker, ingress на 8081/8443);
2. ставит ingress-nginx;
3. собирает 5 образов и загружает их в кластер под тегом `kind`;
4. генерирует `deploy/k8s/base/secret.yaml` из `.env` (файл в `.gitignore`);
5. применяет overlay `deploy/k8s/overlays/kind` — приложение и его зависимости
   (4 × PostgreSQL, Redis, Kafka, MinIO, MailHog) в одном namespace;
6. ждёт rollout и печатает адреса.

Данные в kind-кластере живут в `emptyDir` — при пересоздании подов теряются. Для
постоянного хранения используйте overlay с PVC или операторов.

## 5. Миграции

В Kubernetes миграции вынесены в Job (`deploy/k8s/base/migrations.yaml`), а у Deployment
стоит `SPRING_LIQUIBASE_ENABLED=false`. Job запускает тот же образ с
`--spring.main.web-application-type=none --app.migrate-only=true`: приложение поднимает
контекст, применяет Liquibase и выходит с кодом 0 (хук — `common/MigrationAutoConfiguration`).

Порядок выката: Job → успешное завершение → обновление Deployment.

## 6. CI

`.github/workflows/ci.yml`:

| Job | Что делает |
|---|---|
| build | `./gradlew build` (юнит-тесты, без интеграционных) |
| integration | `./gradlew integrationTest` на Testcontainers |
| images | сборка и пуш 5 образов в GHCR с тегом коммита |
| manifests | `kustomize edit set image` на SHA и проверка `kustomize build` для base и kind |

## 7. Нагрузочные тесты

`bash scripts/bench/run.sh --rebuild` — поднимает стек с нуля, гоняет wrk и складывает
отчёт в `bench-results/<timestamp>/summary.md` (подробности — `scripts/bench/README.md`).
