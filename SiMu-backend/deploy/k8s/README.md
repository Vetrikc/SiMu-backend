# Развёртывание SiMu в Kubernetes

Манифесты для пяти приложений (gateway + 4 сервиса). Stateful-компоненты (PostgreSQL, Kafka, Redis,
MinIO) намеренно **не** описаны: в кластере их ставят операторами или берут managed-сервисы.

## Быстрый старт на k3s

```bash
# 1. кластер (Ubuntu, 4+ ядра, 8 ГБ)
curl -sfL https://get.k3s.io | sh -
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

# 2. ingress-nginx и cert-manager
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml

# 3. секреты
cp deploy/k8s/base/secret.example.yaml deploy/k8s/base/secret.yaml
# отредактировать пароли/ключи, затем:
kubectl apply -k deploy/k8s/base
```

## Stateful-зависимости

| Компонент | Рекомендация |
|---|---|
| PostgreSQL | CloudNativePG или managed (RDS/Cloud SQL) |
| Kafka | Strimzi (KRaft, 3 брокера) или managed |
| Redis | Bitnami chart / managed |
| MinIO | MinIO Operator или внешний S3 |

Для локальной проверки манифестов без операторов можно поднять зависимости через
`docker compose up -d postgres/redis/kafka/minio` на хосте и указать их адреса в ConfigMap
(`AUTH_DB_HOST` и т.д.), если кластер видит хост.

## Пробы

Приложение отдаёт три группы Actuator:

- `/actuator/health/liveness` — процесс жив (не зависит от БД);
- `/actuator/health/readiness` — можно принимать трафик (проверяет БД, Redis);
- `/actuator/health` — полный статус для диагностики.

## Миграции

Liquibase запускается при старте приложения. Для продакшена это ограничение: при `RollingUpdate`
две реплики могут мигрировать одновременно. Варианты:

1. вынести миграции в Job из того же образа (`--spring.main.web-application-type=none` + выход после
   `Liquibase`), а в Deployment выставить `spring.liquibase.enabled=false`;
2. использовать `Recreate` для сервиса с миграциями (простой на время релиза);
3. запускать миграции отдельным шагом CI до `kubectl set image`.

Сейчас выбран компромисс: миграции идемпотентны, а `maxUnavailable: 0` не даёт уронить сервис,
но гонку двух подов на старте это не исключает полностью — пункт 3 рекомендуется как целевой.

## Ресурсы

Запросы/лимиты рассчитаны на 2 реплики каждого сервиса. На 6-ядерной машине полноценный кластер
с stateful-стеком не помещается — нужны внешние БД/брокер или отдельный узел.

## Что дальше

- Grafana Agent / kube-prometheus-stack для сбора `/actuator/prometheus`;
- Tempo/Jaeger для трассировки (`TRACING_ENABLED=true`, `OTLP_ENDPOINT`);
- sealed-secrets или Vault вместо `secret.yaml`;
- ArgoCD/Flux для GitOps-деплоя.
