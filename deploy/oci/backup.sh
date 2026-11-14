#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
backup_dir="$root/backups"
[[ -d "$backup_dir" && ! -L "$backup_dir" ]] || {
  printf 'La carpeta de respaldos es inválida.\n' >&2
  exit 1
}
cd "$root"

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
partial="$backup_dir/main-$stamp.sql.gz.partial"
final="${partial%.partial}"
cleanup() { rm -f -- "$partial"; }
trap cleanup EXIT

docker compose run --rm --no-deps --no-TTY --entrypoint sh database-permissions -c '
  set -eu
  export MYSQL_PWD="$(cat /run/secrets/mysql_operator_password)"
  exec mysqldump --host=mysql --user=main_operator \
    --ssl-mode=VERIFY_IDENTITY --ssl-ca=/run/secrets/mysql_ca \
    --single-transaction --routines --triggers --events --hex-blob \
    --set-gtid-purged=OFF --no-tablespaces main
' | gzip -9 >"$partial"

gzip --test "$partial"
mv "$partial" "$final"
(cd "$backup_dir" && sha256sum "$(basename "$final")" >"$(basename "$final").sha256")
find "$backup_dir" -maxdepth 1 -type f \( -name 'main-*.sql.gz' -o -name 'main-*.sql.gz.sha256' \) -mtime +14 -delete
trap - EXIT
printf 'Respaldo verificado: %s\n' "$final"
