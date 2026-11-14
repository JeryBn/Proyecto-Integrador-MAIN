#!/usr/bin/env bash
set -Eeuo pipefail

[[ $# -eq 1 ]] || { printf 'Uso: ./verify-backup.sh RUTA.sql.gz\n' >&2; exit 2; }
archive="$(realpath -e -- "$1")"
root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
backup_root="$(realpath -e "$root/backups")"
[[ "$archive" == "$backup_root"/main-*.sql.gz && ! -L "$archive" ]] || {
  printf 'El archivo no pertenece a la carpeta de respaldos administrada.\n' >&2
  exit 2
}
[[ -f "$archive.sha256" ]] || { printf 'Falta el manifiesto SHA-256.\n' >&2; exit 1; }
(cd "$backup_root" && sha256sum --check "$(basename "$archive.sha256")")
gzip --test "$archive"
set +o pipefail
header="$(gzip --decompress --stdout "$archive" | head -n 20)"
set -o pipefail
grep -q 'MySQL dump' <<<"$header"
printf 'Respaldo íntegro y legible: %s\n' "$archive"
