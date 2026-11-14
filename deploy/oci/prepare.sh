#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

on_error() {
  printf 'No se pudo preparar el despliegue (línea %s). No se sobrescribieron secretos existentes.\n' "$1" >&2
}
trap 'on_error "$LINENO"' ERR

usage() {
  cat <<'EOF'
Uso: ./prepare.sh --domain HOST [--admin-username USUARIO] [--admin-name NOMBRE]

HOST debe resolver a la IP pública de la VM antes de iniciar Caddy. Puede ser un
dominio propio o un subdominio gratuito. No incluyas https:// ni rutas.
EOF
}

domain=''
admin_username='jery.admin'
admin_name='Jery Becerra Ninaquispe'
while (($#)); do
  case "$1" in
    --domain) domain="${2:-}"; shift 2 ;;
    --admin-username) admin_username="${2:-}"; shift 2 ;;
    --admin-name) admin_name="${2:-}"; shift 2 ;;
    --help|-h) usage; exit 0 ;;
    *) usage >&2; exit 2 ;;
  esac
done

domain="${domain,,}"
[[ "$domain" =~ ^([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}$ ]] || {
  printf 'El dominio no es un hostname público válido.\n' >&2
  exit 2
}
[[ "$domain" != 'main.example.org' && "$domain" != *.example.org ]] || {
  printf 'Reemplaza el dominio de ejemplo por el dominio real.\n' >&2
  exit 2
}
[[ "$admin_username" =~ ^[a-z][a-z0-9_.-]{2,59}$ ]] || {
  printf 'El usuario administrador no cumple el formato permitido.\n' >&2
  exit 2
}
[[ -n "$admin_name" && ${#admin_name} -le 100 && "$admin_name" != *$'\n'* ]] || {
  printf 'El nombre administrativo no es válido.\n' >&2
  exit 2
}

for command in openssl docker; do
  command -v "$command" >/dev/null || { printf 'Falta el comando %s.\n' "$command" >&2; exit 1; }
done
docker compose version >/dev/null

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
secrets="$root/secrets"
mkdir -p "$secrets" "$root/backups"
chmod 0700 "$secrets" "$root/backups"

create_hex_secret() {
  local path="$1"
  if [[ -e "$path" ]]; then
    [[ -f "$path" && ! -L "$path" && "$(<"$path")" =~ ^[a-f0-9]{48}$ ]] || {
      printf 'El secreto existente %s no es válido; no se sobrescribe.\n' "$path" >&2
      exit 1
    }
    return
  fi
  local temporary="${path}.tmp.$$"
  openssl rand -hex 24 >"$temporary"
  chmod 0600 "$temporary"
  mv -n "$temporary" "$path"
}

for name in mysql_root_password mysql_operator_password mysql_migration_password \
  mysql_spring_password mysql_django_password mysql_retention_password django_secret; do
  create_hex_secret "$secrets/$name"
done

if [[ ! -e "$secrets/push_encryption_key" ]]; then
  openssl rand -base64 32 | tr -d '\n' >"$secrets/push_encryption_key"
  printf '\n' >>"$secrets/push_encryption_key"
fi
[[ "$(openssl base64 -d -A -in "$secrets/push_encryption_key" | wc -c)" -eq 32 ]] || {
  printf 'La clave de cifrado push existente no tiene 32 bytes.\n' >&2
  exit 1
}

new_admin_secret=false
if [[ ! -e "$secrets/admin_initial_password" ]]; then
  openssl rand -hex 24 >"$secrets/admin_initial_password"
  new_admin_secret=true
fi
[[ "$(<"$secrets/admin_initial_password")" =~ ^[a-f0-9]{48}$ ]] || {
  printf 'La contraseña administrativa inicial existente no es válida.\n' >&2
  exit 1
}

[[ -e "$secrets/openai_api_key" ]] || : >"$secrets/openai_api_key"
[[ -e "$secrets/firebase-credentials.json" ]] || printf '{}\n' >"$secrets/firebase-credentials.json"

tls_files=(mysql-ca.key mysql-ca.pem mysql-server.key mysql-server.crt)
present=0
for name in "${tls_files[@]}"; do [[ -e "$secrets/$name" ]] && ((present+=1)) || true; done
if ((present != 0 && present != ${#tls_files[@]})); then
  printf 'El juego TLS de MySQL está incompleto; no se regenera parcialmente.\n' >&2
  exit 1
fi
if ((present == 0)); then
  temporary="$(mktemp -d "$secrets/.tls.XXXXXX")"
  trap 'rm -rf "$temporary"' EXIT
  openssl genrsa -out "$temporary/mysql-ca.key" 3072
  openssl req -x509 -new -sha256 -days 3650 \
    -key "$temporary/mysql-ca.key" -subj '/CN=MAIN OCI MySQL CA' \
    -out "$temporary/mysql-ca.pem"
  openssl genrsa -out "$temporary/mysql-server.key" 3072
  openssl req -new -sha256 -key "$temporary/mysql-server.key" \
    -subj '/CN=mysql' -out "$temporary/mysql-server.csr"
  cat >"$temporary/mysql-server.ext" <<'EOF'
subjectAltName=DNS:mysql
extendedKeyUsage=serverAuth
keyUsage=digitalSignature,keyEncipherment
EOF
  openssl x509 -req -sha256 -days 825 \
    -in "$temporary/mysql-server.csr" \
    -CA "$temporary/mysql-ca.pem" -CAkey "$temporary/mysql-ca.key" -CAcreateserial \
    -extfile "$temporary/mysql-server.ext" -out "$temporary/mysql-server.crt"
  for name in "${tls_files[@]}"; do mv "$temporary/$name" "$secrets/$name"; done
  rm -rf "$temporary"
  trap - EXIT
fi

chmod 0600 "$secrets"/*
# Docker Compose implementa secretos de archivo como bind mounts y no puede
# remapear UID/modo. El directorio 0700 protege el host; los archivos montados
# deben ser legibles por los usuarios sin privilegios dentro de cada contenedor.
chmod 0644 \
  "$secrets/mysql_root_password" \
  "$secrets/mysql_operator_password" \
  "$secrets/mysql_migration_password" \
  "$secrets/mysql_spring_password" \
  "$secrets/mysql_django_password" \
  "$secrets/mysql_retention_password" \
  "$secrets/django_secret" \
  "$secrets/push_encryption_key" \
  "$secrets/admin_initial_password" \
  "$secrets/openai_api_key" \
  "$secrets/firebase-credentials.json" \
  "$secrets/mysql-ca.pem" \
  "$secrets/mysql-server.crt" \
  "$secrets/mysql-server.key"

env_tmp="$root/.env.tmp.$$"
cat >"$env_tmp" <<EOF
MAIN_DOMAIN=$domain
MAIN_ADMIN_USERNAME=$admin_username
MAIN_ADMIN_DISPLAY_NAME=$admin_name
MAIN_PUSH_ENABLED=false
MAIN_FCM_CREDENTIALS_FILE=
MAIN_MODERATION_MEDIA_ENABLED=false
MAIN_RETENTION_APPLY=
OPENAI_MODEL=gpt-5-mini
EOF
chmod 0600 "$env_tmp"
mv -f "$env_tmp" "$root/.env"

if [[ "$new_admin_secret" == true || ! -e "$root/initial-access.txt" ]]; then
  cat >"$root/initial-access.txt" <<EOF
URL=https://$domain/admin.html
Usuario=$admin_username
Contraseña inicial=$(<"$secrets/admin_initial_password")
Acción requerida=Cambiar la contraseña al ingresar por primera vez.
EOF
  chmod 0600 "$root/initial-access.txt"
fi

(cd "$root" && docker compose config --quiet)
printf 'Preparación completa para https://%s. El acceso inicial quedó en %s\n' "$domain" "$root/initial-access.txt"
