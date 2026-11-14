#!/usr/bin/env bash
set -Eeuo pipefail

host_pattern="${MAIN_DB_HOST_PATTERN:-172.29.0.%}"
[[ "$host_pattern" == '172.29.0.%' ]] || { printf 'Patrón de red SQL inesperado.\n' >&2; exit 1; }
operator_password="$(</run/secrets/mysql_operator_password)"
[[ "$operator_password" =~ ^[a-f0-9]{48}$ ]] || { printf 'Secreto de operador SQL inválido.\n' >&2; exit 1; }

export MYSQL_PWD="$operator_password"
mysql=(mysql --host=mysql --user=main_operator --ssl-mode=VERIFY_IDENTITY --ssl-ca=/run/secrets/mysql_ca --database=main --batch)

"${mysql[@]}" <<SQL
GRANT SELECT, INSERT, UPDATE ON main.main_user TO 'main_django'@'${host_pattern}';
GRANT SELECT, INSERT, UPDATE ON main.curso TO 'main_django'@'${host_pattern}';
GRANT SELECT, INSERT ON main.alumno TO 'main_django'@'${host_pattern}';
GRANT SELECT, INSERT ON main.docente TO 'main_django'@'${host_pattern}';
GRANT UPDATE(id) ON main.docente TO 'main_django'@'${host_pattern}';
GRANT SELECT, INSERT, DELETE ON main.guardian_link TO 'main_django'@'${host_pattern}';
GRANT SELECT, INSERT, DELETE ON main.timetable TO 'main_django'@'${host_pattern}';
GRANT SELECT, INSERT ON main.contact_link TO 'main_django'@'${host_pattern}';
GRANT SELECT ON main.contact_report TO 'main_django'@'${host_pattern}';
GRANT SELECT ON main.contact_preference TO 'main_django'@'${host_pattern}';
GRANT SELECT, INSERT ON main.actividad TO 'main_django'@'${host_pattern}';
GRANT SELECT, INSERT ON main.administrative_audit TO 'main_django'@'${host_pattern}';
GRANT SELECT, INSERT ON main.notification TO 'main_django'@'${host_pattern}';
GRANT SELECT, INSERT, DELETE ON main.matricula TO 'main_django'@'${host_pattern}';
GRANT SELECT, INSERT, UPDATE ON main.course_grade TO 'main_django'@'${host_pattern}';
GRANT SELECT, UPDATE ON main.message TO 'main_django'@'${host_pattern}';
GRANT SELECT, UPDATE ON main.message_revision TO 'main_django'@'${host_pattern}';
GRANT SELECT, UPDATE ON main.status_update TO 'main_django'@'${host_pattern}';
GRANT SELECT ON main.conversation TO 'main_django'@'${host_pattern}';
GRANT UPDATE(id) ON main.conversation TO 'main_django'@'${host_pattern}';
GRANT SELECT ON main.conversation_member TO 'main_django'@'${host_pattern}';
GRANT SELECT ON main.message_attachment TO 'main_django'@'${host_pattern}';
GRANT SELECT ON main.message_attachment_chunk TO 'main_django'@'${host_pattern}';
GRANT SELECT ON main.status_media_chunk TO 'main_django'@'${host_pattern}';
GRANT SELECT, INSERT, UPDATE ON main.automatic_assessment TO 'main_django'@'${host_pattern}';
GRANT SELECT, INSERT, UPDATE ON main.moderation_retention_hold TO 'main_django'@'${host_pattern}';
GRANT SELECT, UPDATE ON main.moderation_appeal TO 'main_django'@'${host_pattern}';
GRANT SELECT, INSERT, UPDATE, DELETE ON main.django_session TO 'main_django'@'${host_pattern}';

GRANT SELECT ON main.conversation TO 'main_retention'@'${host_pattern}';
GRANT UPDATE(id) ON main.conversation TO 'main_retention'@'${host_pattern}';
GRANT SELECT ON main.message TO 'main_retention'@'${host_pattern}';
GRANT SELECT, UPDATE(body,reason,state,purged_at) ON main.message_revision TO 'main_retention'@'${host_pattern}';
GRANT SELECT, UPDATE(body,reason,state,purged_at) ON main.status_update TO 'main_retention'@'${host_pattern}';
GRANT SELECT, DELETE ON main.message_attachment TO 'main_retention'@'${host_pattern}';
GRANT SELECT, DELETE ON main.message_attachment_chunk TO 'main_retention'@'${host_pattern}';
GRANT SELECT, DELETE ON main.status_media_chunk TO 'main_retention'@'${host_pattern}';
GRANT SELECT, DELETE ON main.status_view TO 'main_retention'@'${host_pattern}';
GRANT SELECT, DELETE ON main.status_recipient TO 'main_retention'@'${host_pattern}';
GRANT SELECT, DELETE ON main.automatic_assessment TO 'main_retention'@'${host_pattern}';
GRANT SELECT, INSERT, DELETE ON main.administrative_audit TO 'main_retention'@'${host_pattern}';
GRANT SELECT, DELETE ON main.moderation_retention_hold TO 'main_retention'@'${host_pattern}';
GRANT SELECT, DELETE ON main.moderation_appeal TO 'main_retention'@'${host_pattern}';
SQL

unset MYSQL_PWD
printf 'Permisos SQL mínimos aplicados.\n'
