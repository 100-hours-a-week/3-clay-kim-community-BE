#!/usr/bin/env bash

set -Eeuo pipefail

log() {
  printf '[deploy] %s\n' "$*"
}

fail() {
  printf '[deploy][error] %s\n' "$*" >&2
  exit 1
}

resolve_compose_command() {
  if docker compose version >/dev/null 2>&1; then
    COMPOSE=(docker compose)
    return
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    COMPOSE=(docker-compose)
    return
  fi

  fail "docker compose is required"
}

check_health() {
  local response

  if command -v curl >/dev/null 2>&1; then
    response="$(curl -fsS "$HEALTH_URL")"
    if [[ -n "$HEALTH_EXPECTED_TEXT" ]]; then
      grep -qF "$HEALTH_EXPECTED_TEXT" <<< "$response"
    fi
    return
  fi

  if command -v wget >/dev/null 2>&1; then
    response="$(wget -qO- "$HEALTH_URL")"
    if [[ -n "$HEALTH_EXPECTED_TEXT" ]]; then
      grep -qF "$HEALTH_EXPECTED_TEXT" <<< "$response"
    fi
    return
  fi

  fail "curl or wget is required for health check"
}

wait_for_health() {
  if [[ "${HEALTH_CHECK_ENABLED}" != "true" ]]; then
    log "Health check skipped"
    return
  fi

  log "Waiting for health check: ${HEALTH_URL}"

  local deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))
  until check_health; do
    if ((SECONDS >= deadline)); then
      "${COMPOSE[@]}" "${COMPOSE_ARGS[@]}" logs --tail=80 "$SERVICE" >&2 || true
      fail "Health check failed within ${HEALTH_TIMEOUT_SECONDS}s"
    fi

    sleep 3
  done
}

REQUESTED_SERVICE="${1:-backend}"
SERVICE="$REQUESTED_SERVICE"
IMAGE_REF="${2:-}"
COMPOSE_FILE="${COMPOSE_FILE:-compose.app.yaml}"
COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-community}"
HEALTH_CHECK_ENABLED="${HEALTH_CHECK_ENABLED:-true}"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-90}"
PRUNE_IMAGES="${PRUNE_IMAGES:-false}"
COMPOSE=()
COMPOSE_ARGS=(--project-name "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE")

if [[ "$SERVICE" == "frontend" ]]; then
  SERVICE="caddy"
fi

if [[ "$SERVICE" != "backend" && "$SERVICE" != "caddy" && "$SERVICE" != "all" ]]; then
  fail "unsupported service: ${SERVICE}"
fi

if [[ "$SERVICE" == "backend" ]]; then
  IMAGE_REF="${IMAGE_REF:-${BACKEND_IMAGE:-}}"
  if [[ -n "$IMAGE_REF" ]]; then
    export BACKEND_IMAGE="$IMAGE_REF"
  fi
  HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:${BACKEND_PORT:-8080}/api/actuator/health}"
  HEALTH_EXPECTED_TEXT="${HEALTH_EXPECTED_TEXT:-\"status\":\"UP\"}"
elif [[ "$SERVICE" == "caddy" ]]; then
  IMAGE_REF="${IMAGE_REF:-${CADDY_IMAGE:-}}"
  if [[ -n "$IMAGE_REF" ]]; then
    export CADDY_IMAGE="$IMAGE_REF"
  fi
  HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:${CADDY_HTTP_PORT:-80}/}"
  HEALTH_EXPECTED_TEXT="${HEALTH_EXPECTED_TEXT:-}"
else
  HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:${BACKEND_PORT:-8080}/api/actuator/health}"
  HEALTH_EXPECTED_TEXT="${HEALTH_EXPECTED_TEXT:-\"status\":\"UP\"}"
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [[ ! -f "$COMPOSE_FILE" ]]; then
  fail "compose file not found: ${SCRIPT_DIR}/${COMPOSE_FILE}"
fi

if [[ ! -f .env && "$SERVICE" == "caddy" ]]; then
  log "Creating empty .env for static frontend deployment"
  touch .env
fi

if [[ ! -f .env ]]; then
  fail "server .env file not found: ${SCRIPT_DIR}/.env"
fi

resolve_compose_command

if [[ -n "${GHCR_USERNAME:-}" && -n "${GHCR_TOKEN:-}" ]]; then
  log "Logging in to GHCR"
  printf '%s' "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin >/dev/null
else
  log "GHCR login skipped; package must be public or already authenticated"
fi

log "Validating compose config"
"${COMPOSE[@]}" "${COMPOSE_ARGS[@]}" config >/dev/null

if [[ "$SERVICE" == "all" ]]; then
  log "Pulling app images"
  "${COMPOSE[@]}" "${COMPOSE_ARGS[@]}" pull

  log "Starting app services"
  "${COMPOSE[@]}" "${COMPOSE_ARGS[@]}" up -d
else
  log "Pulling service: ${SERVICE}${IMAGE_REF:+ (${IMAGE_REF})}"
  "${COMPOSE[@]}" "${COMPOSE_ARGS[@]}" pull "$SERVICE"

  log "Starting service: ${SERVICE}"
  if [[ "$SERVICE" == "caddy" ]]; then
    "${COMPOSE[@]}" "${COMPOSE_ARGS[@]}" up -d --no-deps --force-recreate "$SERVICE"
  else
    "${COMPOSE[@]}" "${COMPOSE_ARGS[@]}" up -d --no-deps "$SERVICE"
  fi
fi

wait_for_health

if [[ "$PRUNE_IMAGES" == "true" ]]; then
  log "Pruning old docker images"
  docker image prune -f --filter "until=168h" >/dev/null
fi

log "Deployment completed: ${REQUESTED_SERVICE}${IMAGE_REF:+ (${IMAGE_REF})}"
