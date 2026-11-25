#!/usr/bin/env bash
# End-to-end benchmark on a Linux host: clone, build, seed, load, collect, report.
#
#   bash scripts/bench/run.sh --repo https://github.com/Vetrikc/SiMu-backend.git
#   bash scripts/bench/run.sh --quick --keep-up
#
# Results land in ./bench-results/<timestamp>/ (summary.md, wrk/, stats/, snapshots/).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$SCRIPT_DIR/lib.sh"

REPO=""
WORKDIR="$HOME/simu-bench"
QUICK=0
KEEP_UP=0
USERS=20
DURATION=60
CONCURRENCY="16 32 64"
LIGHT=0
REBUILD=0

while [ $# -gt 0 ]; do
  case "$1" in
    --repo) REPO="$2"; shift 2;;
    --workdir) WORKDIR="$2"; shift 2;;
    --quick) QUICK=1; DURATION=30; CONCURRENCY="16 64"; shift;;
    --keep-up) KEEP_UP=1; shift;;
    --users) USERS="$2"; shift 2;;
    --duration) DURATION="$2"; shift 2;;
    --concurrency) CONCURRENCY="$2"; shift 2;;
    --light) LIGHT=1; shift;;
    --rebuild) REBUILD=1; shift;;
    -h|--help) sed -n '2,12p' "$0"; exit 0;;
    *) die "unknown option: $1";;
  esac
done

need_cmd docker
need_cmd git
need_cmd curl

# ---------- dependencies ----------
install_deps() {
  local missing=()
  for c in wrk jq python3 curl; do command -v "$c" >/dev/null 2>&1 || missing+=("$c"); done
  if [ ${#missing[@]} -eq 0 ]; then return; fi
  log "installing: ${missing[*]}"
  if command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update -qq
    sudo apt-get install -y -qq wrk jq python3 curl sysstat >/dev/null
  elif command -v dnf >/dev/null 2>&1; then
    sudo dnf install -y wrk jq python3 curl sysstat >/dev/null
  else
    die "unknown package manager; install manually: ${missing[*]}"
  fi
}
install_deps
need_cmd wrk
need_cmd jq

# ---------- repo ----------
ensure_repo "$REPO" "$WORKDIR"
COMPOSE_DIR="$(find "$WORKDIR" -maxdepth 3 -name docker-compose.yml -printf '%h\n' | head -1)"
[ -n "$COMPOSE_DIR" ] || die "docker-compose.yml not found under $WORKDIR"
log "compose dir: $COMPOSE_DIR"

STAMP="$(date +%Y%m%d-%H%M%S)"
RESULTS="$SCRIPT_DIR/../bench-results/$STAMP"
mkdir -p "$RESULTS"/{wrk,stats,snapshots}
log "results: $RESULTS"

# ---------- env ----------
write_env() {
  cd "$COMPOSE_DIR"
  [ -f .env.example ] || die ".env.example missing"
  local jwt internal minio
  jwt="$(random_secret 48)"; internal="$(random_secret 48)"; minio="$(random_secret 20)"
  cp .env.example .env
  sed -i \
    -e "s|^JWT_SECRET=.*|JWT_SECRET=$jwt|" \
    -e "s|^INTERNAL_API_KEY=.*|INTERNAL_API_KEY=$internal|" \
    -e "s|^MINIO_ROOT_PASSWORD=.*|MINIO_ROOT_PASSWORD=$minio|" \
    -e 's|^RATE_LIMIT_.*|&|' \
    -e 's|^RATE_LIMIT_SIGNIN_PER_MINUTE=.*|RATE_LIMIT_SIGNIN_PER_MINUTE=1000000|' \
    -e 's|^RATE_LIMIT_SIGNUP_PER_MINUTE=.*|RATE_LIMIT_SIGNUP_PER_MINUTE=1000000|' \
    -e 's|^RATE_LIMIT_RECOVERY_PER_MINUTE=.*|RATE_LIMIT_RECOVERY_PER_MINUTE=1000000|' \
    -e 's|^RATE_LIMIT_REFRESH_PER_MINUTE=.*|RATE_LIMIT_REFRESH_PER_MINUTE=1000000|' \
    .env
  set -a; . ./.env; set +a
}
write_env

# ---------- start stack ----------
if [ "$REBUILD" = "1" ]; then
  log "clean rebuild: down + build --no-cache"
  docker compose down >/dev/null 2>&1 || true
  docker compose build --no-cache 2>&1 | tail -5
  docker compose up -d 2>&1 | tail -5
else
  log "building and starting the stack (this takes a few minutes)"
  docker compose up -d --build 2>&1 | tail -5
fi
if [ "$LIGHT" = "1" ]; then
  log "light mode: stopping extra replicas"
  docker compose stop api-gateway-2 api-gateway-3 music-service-2 >/dev/null 2>&1 || true
fi
wait_health "http://localhost:${GATEWAY_PORT:-8081}/actuator/health" 420

docker compose ps --format '{{.Name}}|{{.Status}}' > "$RESULTS/compose-ps.txt"
docker version --format '{{.Server.Version}}' > "$RESULTS/docker-version.txt"
nproc > "$RESULTS/nproc.txt"
free -m > "$RESULTS/free.txt"
uname -a > "$RESULTS/uname.txt"

# ---------- seed ----------
BASE="http://localhost:${GATEWAY_PORT:-8081}"
seed() {
  log "seeding $USERS users and 200 tracks"
  TOKEN=""
  local i name resp
  for i in $(seq 1 "$USERS"); do
    name="bench$(date +%s)$i"
    name="${name:0:20}"
    resp=$(curl -fsS -X POST "$BASE/api/v1/auth/signup" -H 'Content-Type: application/json' \
      -d "{\"username\":\"$name\",\"email\":\"$name@example.com\",\"password\":\"Password123\"}") || { warn "signup failed for $name"; continue; }
    [ -z "$TOKEN" ] && TOKEN=$(printf '%s' "$resp" | jq -r .token)
  done
  [ -n "$TOKEN" ] || die "could not obtain an access token"
  printf '%s' "$TOKEN" > "$RESULTS/token.txt"
  docker exec -e PGPASSWORD="${MUSIC_DB_PASSWORD}" simu-music-db psql -U "${MUSIC_DB_USER}" -d "${MUSIC_DB_NAME}" -q \
    -c "insert into music (name, artist, url, created_at, updated_at) select 'Bench ' || g, 'Artist ' || (g % 25), 'http://localhost:9000/simu-music/bench-' || g || '.mp3', now(), now() from generate_series(1, 200) g where not exists (select 1 from music where name = 'Bench ' || g)" \
    >/dev/null || warn "track seeding failed"
}
seed

# ---------- load ----------
THREADS=$(( $(nproc) > 8 ? 8 : $(nproc) ))

warmup() {
  log "warmup ${DURATION}s at c=8"
  wrk -t4 -c8 -d"${DURATION}s" -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/music?limit=50" >/dev/null 2>&1 || true
}
warmup

rm -f "$RESULTS/stats/.stop"
bash "$SCRIPT_DIR/collect.sh" "$RESULTS/stats" 2 > "$RESULTS/stats/collector.log" 2>&1 &
COLLECTOR_PID=$!
trap 'rm -f "$RESULTS/stats/.stop"; kill "$COLLECTOR_PID" 2>/dev/null || true' EXIT

run_wrk() {
  local name="$1" url="$2" conc="$3"
  log "$name  c=$conc  ${DURATION}s"
  date +%s > "$RESULTS/wrk/${name}-c${conc}.start"
  wrk -t"$THREADS" -c"$conc" -d"${DURATION}s" --latency \
    -H "Authorization: Bearer $TOKEN" "$url" \
    > "$RESULTS/wrk/${name}-c${conc}.txt" 2>&1 || warn "wrk failed: $name c=$conc"
  date +%s > "$RESULTS/wrk/${name}-c${conc}.end"
  bash "$SCRIPT_DIR/snapshot.sh" "$RESULTS" "${name}-c${conc}" >/dev/null 2>&1 || true
}

for conc in $CONCURRENCY; do
  run_wrk music    "$BASE/api/v1/music?limit=50" "$conc"
  run_wrk playlists "$BASE/api/v1/me/playlists"   "$conc"
  run_wrk users    "$BASE/api/v1/users"            "$conc"
done

# mixed profile: 55% music / 30% playlists / 15% users
log "mixed profile at c=64"
wrk -t2 -c35 -d"${DURATION}s" --latency -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/music?limit=50" > "$RESULTS/wrk/mixed-music.txt" 2>&1 &
P1=$!
wrk -t2 -c19 -d"${DURATION}s" --latency -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/me/playlists" > "$RESULTS/wrk/mixed-playlists.txt" 2>&1 &
P2=$!
wrk -t1 -c10 -d"${DURATION}s" --latency -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/users" > "$RESULTS/wrk/mixed-users.txt" 2>&1 &
P3=$!
wait "$P1" "$P2" "$P3" || true
bash "$SCRIPT_DIR/snapshot.sh" "$RESULTS" "mixed" >/dev/null 2>&1 || true

rm -f "$RESULTS/stats/.stop"
sleep 2 || true

if [ "$KEEP_UP" = "0" ]; then
  log "stopping the stack"
  docker compose down >/dev/null 2>&1 || true
else
  log "stack left running on $BASE"
fi

python3 "$SCRIPT_DIR/report.py" "$RESULTS" | tee "$RESULTS/summary.md"
log "done: $RESULTS/summary.md"