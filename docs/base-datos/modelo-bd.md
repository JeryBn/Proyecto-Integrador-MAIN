# Modelo compartido

Flyway crea todas las tablas de negocio. Django y Spring comparten main_user y hashes BCrypt: no se usa auth_user ni hashes nativos PBKDF2 de Django. Las contraseñas no son recuperables. Django crea django_session y django_migrations; Flyway mantiene flyway_schema_history.

| Grupo | Responsable de operaciones |
|---|---|
| Cuentas, vínculos, catálogo, años, periodos, aulas, matrículas | Django administra; Spring consulta y autoriza |
| Evaluaciones escolares | Django y Spring docente; se valida versión y acceso |
| Asistencia y recuperación | Django registra; Spring familiar consulta asistencia |
| Publicaciones y confirmaciones | Spring, con rol y alcance por curso/aula |
| Actividades, horario y notas anteriores | Compatibilidad conservada; administración mediante Django |
| Comunicación, avisos y asistente | Spring; revisión/administración correspondiente en Django |

## Inventario generado desde las migraciones SQL

| Tabla | Columnas declaradas |
|---|---|
| `push_device` | id, owner, token_hash, token_cipher, binding, session_hash, session_timeout, registered_at, expires_at |
| `push_delivery` | device_id, notice_id, state, attempts, next_attempt, lease_until |
| `automatic_assessment` | id, policy_version, subject_kind, subject_id, content_hash, outcome, attempts, lease_until, updated_at |
| `reminder_preference` | owner, enabled, minutes_before |
| `moderation_retention_hold` | subject_kind, subject_id, active, reason, opened_by, opened_at, closed_by, closed_at |
| `moderation_appeal` | id, owner, subject_kind, subject_id, revision, content_hash, request_reason, state, created_at, decision_reason, decided_by, decided_at |
| `school_year` | id, name |
| `school_period` | id, year_id, name, position |
| `school_classroom` | id, year_id, level, grade, section, grading_mode, maximum, passing_score, passing_achievement |
| `school_enrollment` | id, year_id, classroom_id, student_id, created_at |
| `school_course` | id, classroom_id, course_id, teacher_username |
| `school_mark` | id, enrollment_id, assignment_id, period_id, assessment, score, achievement, recorded_by, recorded_at, version |
| `school_attendance` | id, enrollment_id, attendance_date, status, note, recorded_by, version |
| `school_recovery` | id, enrollment_id, assignment_id, period_id, plan, due_date, status, result_score, result_achievement, recorded_by, updated_at, version |
| `alumno` | id, usuario, nombre |
| `docente` | id, nombre |
| `curso` | id, nombre, docente_id |
| `matricula` | alumno_id, curso_id |
| `actividad` | id, curso_id, titulo, descripcion, fecha_limite |
| `entrega` | alumno_id, actividad_id, fecha |
| `school_publication` | id, course_id, classroom_id, teacher_username, kind, title, instructions, due_at, created_at |
| `school_publication_read` | publication_id, guardian, read_at |
| `main_user` | username, display_name, role, password_hash, enabled, alumno_id, docente_id, dark_theme, notifications, status_audience |
| `guardian_link` | guardian, alumno_id, verified_by, verified_at |
| `contact_link` | user_a, user_b, approved_by |
| `contact_preference` | owner, subject, muted, blocked |
| `contact_report` | id, reporter, subject, reason, created_at |
| `timetable` | id, curso_id, day_of_week, starts_at, ends_at, classroom |
| `conversation` | id, kind, title, created_by, created_at, direct_key |
| `conversation_member` | conversation_id, username, state, administrator, joined_at |
| `message` | id, conversation_id, sender, client_key, created_at, published_revision, corrected_before, edited_after |
| `message_revision` | message_id, revision, body, state, reason, content_hash, reviewed_by, reviewed_at |
| `assistant_request` | username, client_key, question_hash, state, answer, created_at |
| `notification` | id, recipient, sender, conversation_id, message_id, kind, event_key, created_at, read_at |
| `course_grade` | id, course_id, student_id, assessment, score, maximum, recorded_by, recorded_at |
| `status_update` | id, owner, client_key, body, background, audience, kind, state, content_hash, created_at, expires_at, reason, reviewed_by, reviewed_at |
| `status_media_chunk` | status_id, sequence_no, payload |
| `status_recipient` | status_id, username |
| `status_view` | status_id, username, viewed_at |
| `administrative_audit` | id, actor, action, subject, occurred_at |
| `message_attachment` | message_id, kind, mime, file_name, byte_size, source_hash, content_hash |
| `message_attachment_chunk` | message_id, sequence_no, payload |

Las alteraciones posteriores agregan, entre otros, archived y password_change_required en main_user, competency en school_mark y active_period_id en school_year. V24 ajusta las fechas de publicaciones para MySQL/MariaDB. Consultar las migraciones completas para índices, claves y restricciones.

Relaciones centrales: alumno→matricula→curso→actividad; alumno→school_enrollment→school_classroom→school_course; school_mark enlaza matrícula, asignación y periodo; guardian_link limita cada familiar a sus alumnos; school_publication_read registra el familiar lector.
