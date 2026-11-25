#!/usr/bin/env bash
# Shared helpers for the benchmark scripts.
set -euo pipefail

log()  { printf '\033[1;34m[bench]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[warn]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[fail]\033[0m %s\n' "$*" >&2; exit 1; }

need_cmd() { command -v "$1" >/dev/null 2>&1 || die "command not found: $1"; }

# wait_health <url> <timeout-seconds>
wait_health() {
  local url="$1" timeout="${2:-300}" waited=0
  log "waiting for $url"
  until curl -fsS --max-time 3 "$url" >/dev/null 2>&1; do
    waited=$((waited + 3))
    if [ "$waited" -ge "$timeout" ]; then
      die "service did not become healthy in ${timeout}s: $url"
    fi
    sleep 3
  done
  log "healthy after ${waited}s"
}

# random_secret <length>
random_secret() {
  local len="${1:-40}"
  LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c "$len"
}

# json_field <json> <jq-path>
json_field() { printf '%s' "$1" | python3 -c 'import json,sys; d=json.load(sys.stdin); print(eval("d"+sys.argv[1]))' "$2"; }

# ensure_repo <repo-url> <dir>
ensure_repo() {
  local repo="$1" target="$2"
  if [ -d "$target/.git" ]; then
    log "repository already present, updating"
    git -C "$target" fetch --all --prune
    git -C "$target" pull --ff-only || warn "pull skipped (local changes?)"
  elif [ -n "$repo" ]; then
    log "cloning $repo"
    git clone "$repo" "$target"
  else
    die "no repository at $target and no --repo url given"
  fi
}
