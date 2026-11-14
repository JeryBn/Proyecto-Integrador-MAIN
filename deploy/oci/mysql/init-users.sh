#!/usr/bin/env bash
set -Eeuo pipefail

read_secret() {
  local path="$1"
  [[ -r "$path" ]] || { printf 'Falta %s\n' "$path" >&2; exit 1; }
  local value
  value="$(<"$path")"
  [[ "$value" =~ ^[a-f0-9]{48}$ ]] || { printf 'Secreto SQL inválido: %s\n' "$path" >&2; exit 1; }
  printf '%s' "$value"
}

operator_password="$(read_secret /run/secrets/mysql_operator_password)"
migration_password="$(read_secret /run/secrets/mysql_migration_password)"
spring_password="$(read_secret /run/secrets/mysql_spring_password)"
django_password="$(read_secret /run/secrets/mysql_django_password)"
retention_password="$(read_secret /run/secrets/mysql_retention_password)"
host_pattern='172.29.0.%'

mysql --protocol=socket --user=root --password="${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE USER IF NOT EXISTS 'main_operator'@'${host_pattern}' IDENTIFIED BY '${operator_password}' REQUIRE SSL;
CREATE USER IF NOT EXISTS 'main_migration'@'${host_pattern}' IDENTIFIED BY '${migration_password}' REQUIRE SSL;
CREATE USER IF NOT EXISTS 'main_spring'@'${host_pattern}' IDENTIFIED BY '${spring_password}' REQUIRE SSL;
CREATE USER IF NOT EXISTS 'main_django'@'${host_pattern}' IDENTIFIED BY '${django_password}' REQUIRE SSL;
CREATE USER IF NOT EXISTS 'main_retention'@'${host_pattern}' IDENTIFIED BY '${retention_password}' REQUIRE SSL;

GRANT ALL PRIVILEGES ON main.* TO 'main_operator'@'${host_pattern}' WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON main.* TO 'main_migration'@'${host_pattern}';
GRANT SELECT, INSERT, UPDATE, DELETE ON main.* TO 'main_spring'@'${host_pattern}';
FLUSH PRIVILEGES;
SQL
