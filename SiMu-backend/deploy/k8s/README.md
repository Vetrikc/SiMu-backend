# Развёртывание SiMu в Kubernetes

## Вариант 1: локально на kind (проверка манифестов)

```bash
bash scripts/k8s/up.sh
kubectl -n simu get pods
curl http://localhost:8081/actuator/health
```

Скрипт создаёт кластер `simu`, ставит ingress-nginx, собирает образы, загружает их в kind,
генерирует секрет из `.env` и применяет overlay `overlays/kind`: приложение и зависимости
(PostgreSQL, Redis, Kafka, MinIO, MailHog) в одном namespace, данные в `emptyDir`.

Требования: 6+ ядер, 10+ ГБ свободной памяти, kind и kubectl.

## Вариант 2: кластер (k3s / managed)

```bash
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
kubectl apply -f deploy/k8s/base/namespace.yaml
bash scripts/k8s/secret.sh              # генерирует base/secret.yaml из .env (не коммитится)
kubectl apply -f deploy/k8s/base/secret.yaml
kubectl apply -f deploy/k8s/base/migrations.yaml
kubectl wait --for=condition=complete job -l role=migration -n simu --timeout=300s
kubectl apply -k deploy/k8s/base
```

Stateful-компоненты (PostgreSQL, Kafka, Redis, MinIO) в base намеренно не описаны:
в кластере их ставят операторами (CloudNativePG, Strimzi, Bitnami) или берут managed.
В `configmap.yaml` подставьте реальные адреса.

## Миграции

Deployment'ы стартуют с `SPRING_LIQUIBASE_ENABLED=false`. Схему применяют Job'ы из
`migrations.yaml` — тот же образ с `--app.migrate-only=true` (хук в модуле `common`):
контекст поднимается, Liquibase отрабатывает, процесс выходит с кодом 0.
Порядок выката: Job → complete → rolling update.

## Пробы

- `/actuator/health/liveness` — процесс жив;
- `/actuator/health/readiness` — БД и Redis доступны;
- `/actuator/health` — полный статус.

## Образы

CI пушит `ghcr.io/<owner>/simu-<module>:<sha>`. В манифестах стоит `:latest`;
в пайплайне тег подставляется через `kustomize edit set image` (шаг `manifests`).

## Что дальше

- `kube-prometheus-stack`: аннотации `prometheus.io/scrape` уже стоят на Deployment;
- Tempo/Jaeger для трейсинга (`TRACING_ENABLED=true`, `OTLP_ENDPOINT`);
- sealed-secrets или Vault вместо `secret.yaml`;
- ArgoCD/Flux для GitOps.
