#!/usr/bin/env bash
set -Eeuo pipefail

fail() {
  printf 'Spring no pudo iniciar: %s\n' "$1" >&2
  exit 1
}

read_secret() {
  local variable="$1"
  local file_variable="$2"
  local path="${!file_variable:-}"
  [[ -n "$path" && -r "$path" ]] || fail "falta el secreto $file_variable"
  local value
  value="$(<"$path")"
  [[ -n "$value" ]] || fail "el secreto $file_variable está vacío"
  printf -v "$variable" '%s' "$value"
  export "$variable"
}

read_optional_secret() {
  local variable="$1"
  local file_variable="$2"
  local path="${!file_variable:-}"
  [[ -n "$path" && -r "$path" ]] || return 0
  local value
  value="$(<"$path")"
  [[ -n "$value" ]] || return 0
  printf -v "$variable" '%s' "$value"
  export "$variable"
}

read_secret MAIN_DB_PASSWORD MAIN_DB_PASSWORD_FILE
read_secret SPRING_FLYWAY_PASSWORD SPRING_FLYWAY_PASSWORD_FILE
read_secret MAIN_PUSH_ENCRYPTION_KEY MAIN_PUSH_ENCRYPTION_KEY_FILE
read_optional_secret OPENAI_API_KEY OPENAI_API_KEY_FILE

ca_file="${MAIN_MYSQL_CA_FILE:-}"
[[ -n "$ca_file" && -r "$ca_file" ]] || fail 'falta la CA privada de MySQL'
trust_store=/tmp/main-mysql-truststore.p12
rm -f "$trust_store"
keytool -importcert -noprompt -alias main-mysql-ca -file "$ca_file" \
  -keystore "$trust_store" -storetype PKCS12 -storepass changeit >/dev/null
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djavax.net.ssl.trustStore=$trust_store -Djavax.net.ssl.trustStorePassword=changeit"

exec java -jar /app/app.jar
