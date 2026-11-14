#!/usr/bin/env bash
set -Eeuo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
cd "$root"

on_error() {
  status=$?
  printf 'El despliegue falló (línea %s, código %s). Estado actual:\n' "$1" "$status" >&2
  docker compose ps >&2 || true
  docker compose logs --tail=80 --no-color >&2 || true
  exit "$status"
}
trap 'on_error "$LINENO"' ERR

[[ -f .env && -d secrets ]] || {
  printf 'Ejecuta prepare.sh antes de desplegar.\n' >&2
  exit 2
}
domain="$(sed -n 's/^MAIN_DOMAIN=//p' .env)"
[[ -n "$domain" ]] || { printf 'Falta MAIN_DOMAIN en .env.\n' >&2; exit 2; }

docker compose config --quiet
docker compose pull --quiet mysql database-permissions caddy
docker compose build --pull spring django
docker compose up --detach --remove-orphans --wait --wait-timeout 360

deadline=$((SECONDS + 180))
until curl --fail --silent --show-error --max-time 10 "https://$domain/actuator/health" | grep -q '"status":"UP"'; do
  ((SECONDS < deadline)) || {
    printf 'Los contenedores iniciaron, pero HTTPS público no respondió correctamente. Revisa DNS y puertos 80/443.\n' >&2
    exit 1
  }
  sleep 5
done

curl --fail --silent --show-error --max-time 10 "https://$domain/api/admin/health" >/dev/null
printf 'MAIN está disponible y saludable en https://%s\n' "$domain"
printf 'Acceso inicial privado: %s/initial-access.txt\n' "$root"
