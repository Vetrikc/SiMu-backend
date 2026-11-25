#!/usr/bin/env bash
# Builds deploy/k8s/base/secret.yaml from the repository .env (never committed).
# Usage: bash scripts/k8s/secret.sh [path-to-.env]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="${1:-$REPO/.env}"
OUT="$REPO/deploy/k8s/base/secret.yaml"

[ -f "$ENV_FILE" ] || { echo "missing $ENV_FILE" >&2; exit 1; }
command -v kubectl >/dev/null || { echo "kubectl not found" >&2; exit 1; }

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

kubectl create secret generic simu-secrets \
  --namespace simu \
  --from-literal=AUTH_DB_USER="${AUTH_DB_USER:-simu}" \
  --from-literal=AUTH_DB_PASSWORD="${AUTH_DB_PASSWORD:?AUTH_DB_PASSWORD missing}" \
  --from-literal=USER_DB_USER="${USER_DB_USER:-simu}" \
  --from-literal=USER_DB_PASSWORD="${USER_DB_PASSWORD:?USER_DB_PASSWORD missing}" \
  --from-literal=MUSIC_DB_USER="${MUSIC_DB_USER:-simu}" \
  --from-literal=MUSIC_DB_PASSWORD="${MUSIC_DB_PASSWORD:?MUSIC_DB_PASSWORD missing}" \
  --from-literal=PLAYLIST_DB_USER="${PLAYLIST_DB_USER:-simu}" \
  --from-literal=PLAYLIST_DB_PASSWORD="${PLAYLIST_DB_PASSWORD:?PLAYLIST_DB_PASSWORD missing}" \
  --from-literal=JWT_SECRET="${JWT_SECRET:?JWT_SECRET missing}" \
  --from-literal=INTERNAL_API_KEY="${INTERNAL_API_KEY:?INTERNAL_API_KEY missing}" \
  --from-literal=S3_ACCESS_KEY="${MINIO_ROOT_USER:-simu}" \
  --from-literal=S3_SECRET_KEY="${MINIO_ROOT_PASSWORD:-change_me}" \
  --from-literal=GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-}" \
  --from-literal=GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET:-}" \
  --from-literal=GOOGLE_REDIRECT_URI="${GOOGLE_REDIRECT_URI:-http://localhost:8081/api/v1/auth/oauth2/google/callback}" \
  --from-literal=OAUTH_FRONTEND_REDIRECT_URI="${OAUTH_FRONTEND_REDIRECT_URI:-http://localhost:8090/oauth2/callback}" \
  --dry-run=client -o yaml > "$OUT"

echo "written $OUT"
