#!/usr/bin/env bash
# Background sampler: docker stats, CPU per core, memory, network, disk.
# Usage: collect.sh <output-dir> [interval-seconds]
set -uo pipefail

OUT="$1"
INTERVAL="${2:-2}"
STOP_FILE="$OUT/.stop"
mkdir -p "$OUT"

DOCKER_CSV="$OUT/docker-stats.csv"
MPSTAT_LOG="$OUT/mpstat.log"
VMSTAT_LOG="$OUT/vmstat.log"
NET_LOG="$OUT/net.log"
MEM_LOG="$OUT/memory.log"

[ -f "$DOCKER_CSV" ] || echo "epoch,container,cpu_percent,mem_usage,mem_limit,net_rx,net_tx,block_read,block_write" > "$DOCKER_CSV"

while [ ! -f "$STOP_FILE" ]; do
  ts=$(date +%s)
  docker stats --no-stream --format '{{.Name}}|{{.CPUPerc}}|{{.MemUsage}}|{{.NetIO}}|{{.BlockIO}}' 2>/dev/null \
  | while IFS='|' read -r name cpu mem net block; do
      [ -z "$name" ] && continue
      cpu_clean=${cpu%\%}
      mem_used=${mem%% *}; mem_limit=${mem##*/ }
      net_rx=$(echo "$net" | cut -d/ -f1 | tr -d ' ')
      net_tx=$(echo "$net" | cut -d/ -f2 | tr -d ' ')
      block_r=$(echo "$block" | cut -d/ -f1 | tr -d ' ')
      block_w=$(echo "$block" | cut -d/ -f2 | tr -d ' ')
      echo "$ts,$name,$cpu_clean,$mem_used,$mem_limit,$net_rx,$net_tx,$block_r,$block_w" >> "$DOCKER_CSV"
    done
  if command -v mpstat >/dev/null 2>&1; then
    mpstat -P ALL 1 1 2>/dev/null | tail -n +4 | sed "s/^/$ts /" >> "$MPSTAT_LOG"
  fi
  if command -v vmstat >/dev/null 2>&1; then
    vmstat 1 2 2>/dev/null | tail -1 | sed "s/^/$ts /" >> "$VMSTAT_LOG"
  fi
  if command -v sar >/dev/null 2>&1; then
    sar -n DEV 1 1 2>/dev/null | tail -n +4 | grep -v '^$' | sed "s/^/$ts /" >> "$NET_LOG"
  fi
  { echo "$ts $(free -m | sed -n '2p')"; } >> "$MEM_LOG"
  sleep "$INTERVAL"
done
