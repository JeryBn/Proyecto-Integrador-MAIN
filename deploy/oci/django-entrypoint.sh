#!/usr/bin/env bash
set -Eeuo pipefail

fail() {
  printf 'Django no pudo iniciar: %s\n' "$1" >&2
  exit 1
}

read_secret() {
  local variable="$1"
  local path="$2"
  [[ -n "$path" && -r "$path" ]] || fail "falta el archivo privado para $variable"
  local value
  value="$(<"$path")"
  [[ -n "$value" ]] || fail "el secreto $variable está vacío"
  printf -v "$variable" '%s' "$value"
  export "$variable"
}

read_optional_secret() {
  local variable="$1"
  local path="$2"
  [[ -n "$path" && -r "$path" ]] || return 0
  local value
  value="$(<"$path")"
  [[ -n "$value" ]] || return 0
  printf -v "$variable" '%s' "$value"
  export "$variable"
}

read_secret MAIN_DJANGO_SECRET_KEY "${MAIN_DJANGO_SECRET_KEY_FILE:-}"

mode="${1:-serve}"
case "$mode" in
  migrate)
    export MAIN_DB_USER=main_migration
    read_secret MAIN_DB_PASSWORD /run/secrets/mysql_migration_password
    exec python manage.py migrate --noinput
    ;;
  provision-admin)
    export MAIN_DB_USER=main_django
    read_secret MAIN_DB_PASSWORD /run/secrets/mysql_django_password
    read_secret MAIN_ADMIN_INITIAL_PASSWORD /run/secrets/admin_initial_password
    exec python manage.py provision_admin \
      --username "${MAIN_ADMIN_USERNAME:?falta MAIN_ADMIN_USERNAME}" \
      --name "${MAIN_ADMIN_DISPLAY_NAME:?falta MAIN_ADMIN_DISPLAY_NAME}"
    ;;
  verify)
    export MAIN_DB_USER=main_django
    read_secret MAIN_DB_PASSWORD /run/secrets/mysql_django_password
    exec python manage.py verify_deployment \
      --username "${MAIN_ADMIN_USERNAME:?falta MAIN_ADMIN_USERNAME}"
    ;;
  serve)
    export MAIN_DB_USER=main_django
    read_secret MAIN_DB_PASSWORD /run/secrets/mysql_django_password
    read_optional_secret OPENAI_API_KEY "${OPENAI_API_KEY_FILE:-}"
    exec python -m waitress \
      --listen=0.0.0.0:8090 \
      --threads=4 \
      --trusted-proxy=172.29.0.2 \
      --trusted-proxy-headers=x-forwarded-proto \
      main_admin.wsgi:application
    ;;
  retention)
    export MAIN_DB_USER=main_retention
    read_secret MAIN_DB_PASSWORD /run/secrets/mysql_retention_password
    exec python manage.py retain_content --apply --watch --limit 100
    ;;
  *)
    fail "modo desconocido: $mode"
    ;;
esac
