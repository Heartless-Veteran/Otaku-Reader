#!/usr/bin/env bash
# SessionStart hook: ensure ruflo is initialised in this container.
#
# Why this exists: remote-execution containers are ephemeral. The repo carries the *config
# skeleton* (.mcp.json, .claude/settings.json, .claude/hooks/, .claude-flow/config.yaml),
# but ruflo's runtime stores (.claude-flow/data/, sessions/, logs/) and the agents/commands/
# helpers scaffolding are .gitignored and have to be regenerated on each fresh container.
# This hook does that regeneration idempotently: if the runtime is already present, it's a
# no-op fast path; otherwise it runs `ruflo init` to recreate everything before the user's
# first request lands.
#
# Output goes to stderr only (so it doesn't pollute hook capture). One "ruflo: ready" line
# on success, full error block on failure.

set -euo pipefail

# Allow override for testing.
REPO_ROOT="${CLAUDE_PROJECT_DIR:-$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)}"
cd "$REPO_ROOT"

log() { printf '[ruflo-bootstrap] %s\n' "$*" >&2; }

# Fast path: ruflo is initialised when both of these are present. Matches the markers
# ruflo's own "already initialized" check uses (see `ruflo init` output).
if [[ -f .claude/settings.json && -f .claude-flow/config.yaml ]]; then
    log "ruflo: ready (cached)"
    exit 0
fi

log "ruflo: runtime missing, initialising…"

# npx itself is the typical failure mode. Prefer the in-cache binary if it's still intact
# (faster, survives the stale-rename `ENOTEMPTY` case where `npx -y` would fight the cache);
# otherwise fall back to a `--yes` npx that re-fetches.
RUFLO_CACHED_BIN=""
for candidate in /root/.npm/_npx/*/node_modules/ruflo/bin/ruflo.js; do
    if [[ -f "$candidate" ]]; then
        RUFLO_CACHED_BIN="$candidate"
        break
    fi
done

# ruflo init exits non-zero when it detects an existing init (without --force) — treat that
# as success because the hook is idempotent by design.
INIT_EXIT=0
if [[ -n "$RUFLO_CACHED_BIN" ]]; then
    log "using cached ruflo at $RUFLO_CACHED_BIN"
    node "$RUFLO_CACHED_BIN" init --minimal --no-global --skip-claude >&2 || INIT_EXIT=$?
else
    log "no cached ruflo; falling back to npx -y ruflo@latest"
    npx -y ruflo@latest init --minimal --no-global --skip-claude >&2 || INIT_EXIT=$?
fi

if [[ "$INIT_EXIT" -ne 0 ]] && [[ ! -f .claude-flow/config.yaml ]]; then
    log "ruflo: init failed (exit $INIT_EXIT) and no config present — escalate"
    exit "$INIT_EXIT"
fi

log "ruflo: ready"
