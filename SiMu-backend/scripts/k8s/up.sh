#!/usr/bin/env bash
# Local Kubernetes (kind) with the application and its dependencies in one cluster.
# Usage: bash scripts/k8s/up.sh [--skip-build]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$SCRIPT_DIR/../.." && pwd)"
CLUSTER=simu
OVERLAY="$REPO/deploy/k8s/overlays/kind"

SKIP_BUILD=0
[ "${1:-}" = "--skip-build" ] && SKIP_BUILD=1

for cmd in kind kubectl docker; do
  command -v "$cmd" >/dev/null || { echo "missing $cmd" >&2; exit 1; }
done

if ! kind get clusters 2>/dev/null | grep -qx "$CLUSTER"; then
  echo "==> creating kind cluster"
  kind create cluster --config "$OVERLAY/kind-config.yaml"
fi

echo "==> installing ingress-nginx"
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl -n ingress-nginx wait --for=condition=Available deployment/ingress-nginx-controller --timeout=180s

if [ "$SKIP_BUILD" = "0" ]; then
  echo "==> building images"
  docker compose -f "$REPO/docker-compose.yml" build api-gateway auth-service user-service music-service playlist-service
fi

echo "==> loading images into kind"
for m in api-gateway auth-service user-service music-service playlist-service; do
  image=$(docker compose -f "$REPO/docker-compose.yml" config --images 2>/dev/null | grep -m1 "$m" || true)
  docker tag "simu-$m" "simu-$m:kind" 2>/dev/null || true
  kind load docker-image "simu-$m:kind" --name "$CLUSTER"
done

echo "==> namespace and secrets"
kubectl apply -f "$REPO/deploy/k8s/base/namespace.yaml"
bash "$SCRIPT_DIR/secret.sh"
kubectl apply -f "$REPO/deploy/k8s/base/secret.yaml"

echo "==> applying overlay"
kubectl apply -k "$OVERLAY"

echo "==> waiting for stateful dependencies"
kubectl -n simu rollout status deploy/auth-db --timeout=180s
kubectl -n simu rollout status deploy/user-db --timeout=180s
kubectl -n simu rollout status deploy/music-db --timeout=180s
kubectl -n simu rollout status deploy/playlist-db --timeout=180s
kubectl -n simu rollout status deploy/redis --timeout=120s
kubectl -n simu rollout status deploy/kafka --timeout=180s
kubectl -n simu rollout status deploy/minio --timeout=120s

echo "==> waiting for services"
kubectl -n simu rollout status deploy/api-gateway --timeout=300s
kubectl -n simu rollout status deploy/auth-service --timeout=300s
kubectl -n simu rollout status deploy/user-service --timeout=300s
kubectl -n simu rollout status deploy/music-service --timeout=300s
kubectl -n simu rollout status deploy/playlist-service --timeout=300s

cat <<EOF

Cluster ready.
  API (ingress): http://localhost:8081/actuator/health
  Get a token:   curl -s -X POST http://localhost:8081/api/v1/auth/signup \\
                   -H 'Content-Type: application/json' \\
                   -d '{"username":"kinduser","email":"kind@example.com","password":"Password123"}'
  Pods:           kubectl -n simu get pods
EOF
