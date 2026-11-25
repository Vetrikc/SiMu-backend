#!/usr/bin/env bash
# One-shot snapshot of application and dependency state. Usage: snapshot.sh <dir> <label>
set -uo pipefail
OUT="$1"
LABEL="$2"
DEST="$OUT/snapshots/$LABEL"
mkdir -p "$DEST"

for port in 8081 8082 8083 8084 8085; do
  curl -fsS --max-time 5 "http://localhost:$port/actuator/health" > "$DEST/health-$port.json" 2>/dev/null || true
  curl -fsS --max-time 10 "http://localhost:$port/actuator/prometheus" > "$DEST/prometheus-$port.txt" 2>/dev/null || true
done

docker exec simu-redis redis-cli INFO 2>/dev/null > "$DEST/redis-info.txt" || true

docker exec -e PGPASSWORD="$AUTH_DB_PASSWORD" simu-auth-db psql -U "$AUTH_DB_USER" -d "$AUTH_DB_NAME" -c \
  'select state, count(*) from pg_stat_activity group by 1' > "$DEST/pg-auth-activity.txt" 2>/dev/null || true
docker exec -e PGPASSWORD="$MUSIC_DB_PASSWORD" simu-music-db psql -U "$MUSIC_DB_USER" -d "$MUSIC_DB_NAME" -c \
  "select relname, seq_scan, idx_scan, n_tup_ins, n_tup_upd from pg_stat_user_tables order by seq_scan desc" \
  > "$DEST/pg-music-tables.txt" 2>/dev/null || true

if docker ps --format '{{.Names}}' | grep -q '^simu-kafka$'; then
  docker exec simu-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --all-groups \
    > "$DEST/kafka-groups.txt" 2>/dev/null || true
  docker exec simu-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe \
    > "$DEST/kafka-topics.txt" 2>/dev/null || true
fi

docker compose ps --format '{{.Name}}|{{.Status}}' > "$DEST/compose-ps.txt" 2>/dev/null || true
echo "snapshot: $LABEL"

date +%s > "$DEST/taken-at.txt"
