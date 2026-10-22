# Migración administrativa a Django — 22 de octubre de 2026

Continuación asistida solicitada por Jery Becerra Ninaquispe. Se conserva el trabajo original de Michael Julian Fernandez Camarena (contratos Spring) y Pamela Regina Meneses Mayhua (interfaz React).

## Incremento: cursos, matrículas y auditoría

- Django implementa directamente `/api/admin/v1/courses`, edición por ID, listado/cambio de matrículas y consulta de auditoría. No actúa como proxy de Spring.
- Mantiene el contrato React, autenticación administrativa, CSRF, paginación, docente activo, versión optimista del curso, bloqueo de reasignación con horario y operaciones de matrícula idempotentes.
- Cambios y auditoría se guardan en una transacción. La auditoría es de solo lectura por API; no expone hashes ni claves.
- Flyway conserva la propiedad del esquema de negocio; no se recrearon tablas ni se borraron datos existentes.
- Verificación: 20 pruebas Django correctas en SQLite desechable, incluidas cinco nuevas; 13 comprobaciones HTTP reales Django → MySQL → Spring. Se verificó que una matrícula nueva aparece y su revocación desaparece de la agenda de Spring en la misma sesión.
- Evidencia: `docs/informes/2026-10-22-django/academic-http.json`. El curso sintético de esa evidencia permanece para inspección; la matrícula de prueba quedó revocada.

## Incremento: cuentas, parentescos y contactos

- Altas de alumno/docente/familiar, consulta segura de cuentas, restablecimiento de contraseña, deshabilitación, parentescos, contactos autorizados y reportes migrados directamente a Django.
- No permite crear ni modificar administradores desde este módulo. Conserva BCrypt compatible y la identidad académica única; cambios y auditoría son atómicos.
- Spring valida en cada petición autenticada la huella de contraseña, rol y habilitación del principal contra la identidad compartida. Una acción Django revoca la sesión Spring existente, sin depender de memoria de otro proceso.
- Verificación: 24 pruebas Django, 102 pruebas Maven (tres nuevas sobre revocación SQL) y 17 comprobaciones HTTP reales con cuenta familiar sintética, visibilidad/revocación de parentesco, contacto, cambio de contraseña y deshabilitación. Cuenta de prueba final deshabilitada.
- Evidencia: `docs/informes/2026-10-22-django/identity-http.json`.

## Incremento: horarios y notas administrativas

- Django registra/retira horarios y registra/edita notas por curso. Conserva el bloqueo por curso/docente, detección de cruces, matrícula requerida, escala de hasta 1000 y dos decimales, versión temporal de nota y auditoría atómica.
- Las rutas docentes y estudiantiles permanecen en Spring.
- 27 pruebas Django aprobadas y 15 verificaciones HTTP reales sobre MySQL: horario compartido, cruce rechazado, nota creada/editada, versión obsoleta rechazada y nota oculta tras revocar matrícula.
- Evidencia `docs/informes/2026-10-22-django/scheduling-grades-http.json`. Conserva curso y nota sintéticos; retira horario y matrícula de esa prueba.
- La primera ejecución detectó un permiso insuficiente de bloqueo de `docente` en MySQL; se otorgó únicamente UPDATE de la columna id en esa tabla del esquema aislado y se repitió el recorrido completo correctamente. Se revocó también la matrícula sintética de la primera ejecución; se conservan curso y auditoría.

## Incremento de moderación y separación de servidores

- Django ofrece las colas de mensajes/estados, revisión de versión/huella, rechazo y aprobación humana explícita, descarga autorizada de archivos y verificación de integridad. La imagen/video/audio de un estado requiere confirmación de revisión completa, igual que el adjunto de mensaje.
- Una aprobación comprueba emisor activo, membresía vigente y contactos/bloqueos en privados. Publicación, notificación y auditoría se confirman juntas; conserva las marcas de corrección/edición y evita notificación duplicada.
- Los pendientes, rechazados y revisiones obsoletas no se publican. Estados caducados quedan fuera de revisión. Los archivos se entregan con `no-store` y `nosniff`; su descarga administrativa deja de estar disponible al terminar la revisión.
- No se invoca IA ni se autoaprueba contenido cuando faltan proveedor o cuota. No se ha designado revisor de apelaciones ni retención.
- El iniciador cierra la administración histórica Spring mediante `MAIN_ADMIN_LEGACY_ENABLED=false`. Java conserva el código original para compatibilidad explícita; su API de usuarios sigue activa. Dos nuevas pruebas verifican este límite.
- Pruebas finales: 104 Maven, 32 Django y 38 comprobaciones HTTP reales de moderación, además de las 13/17/15 por módulo y 16 de persistencia original tras reiniciar Spring. Las suites se describen separadamente; no equivalen a aceptación del profesor.
- También pasaron 6 verificaciones HTTP del cierre administrativo Spring, 4 pruebas del formulario web y 3 pruebas instrumentadas Android sobre el servidor final y MySQL (SharedArchitectureUiTest y DetailContractTest). El primer intento móvil no encontró dispositivo tras cerrarse el emulador; al reabrir con `-gpu software` se repitió y aprobó. La app quedó reinstalada y abierta.
- React comprobado en Chrome contra Django: navegación a comunidad y cursos/auditoría, carga de cola y rechazo de una revisión sintética desde la interfaz. Captura de pantalla no disponible en esa ejecución por fallo de captura del navegador; no se sustituye por una imagen inventada.

## Contratos y permisos

Todos los endpoints siguientes requieren sesión ADMIN Django y CSRF para escrituras. Los GET de listas v1 retornan arrays; `offset` 0–1000000 y `limit` 1–100. Los endpoints originales `/cursos` y `/actividades` conservan su envoltorio paginado.

| Recurso bajo `/api/admin/v1` | Métodos |
| --- | --- |
| users | GET y POST |
| users/{username}/reset-password y /disable | POST |
| guardians y guardians/{guardian}/{studentId} | POST y DELETE respectivamente |
| contacts y reports | POST y GET respectivamente |
| courses y courses/{id} | GET/POST y PUT respectivamente |
| courses/{id}/students | GET y PUT |
| courses/{id}/grades | PUT |
| timetable y timetable/{id} | POST y DELETE respectivamente |
| audit | GET solamente |
| moderation/messages y moderation/statuses | GET |
| moderation/messages/{id}/review y moderation/statuses/{id}/review | POST |
| moderation/messages/{id}/attachment?revision={n} y moderation/statuses/{id}/media | GET |

El JSON de escritura rechaza campos desconocidos. Errores bajo `error.code/message`: 400 validación, 401 sesión ausente/revocada, 403 CSRF o acceso, 404 recurso ausente/no disponible, 409 conflicto de versión/datos/integridad. Actividades conserva 415 para tipo de contenido incorrecto. BCrypt limita contraseña a 72 bytes UTF-8 y exige 12 caracteres; nunca se devuelve el hash.

Cuenta SQL Django sin DDL ni permisos globales: SELECT/INSERT/UPDATE en main_user y curso; SELECT/INSERT en alumno y docente; UPDATE(id) en docente y conversation para bloqueos; SELECT/INSERT/DELETE en guardian_link y timetable; SELECT/INSERT en contact_link, actividad, administrative_audit y notification; SELECT/INSERT/DELETE en matricula; SELECT/INSERT/UPDATE en course_grade; SELECT/UPDATE en message, message_revision y status_update; SELECT en conversation_member, contact_preference, contact_report, message_attachment, message_attachment_chunk y status_media_chunk; CRUD solamente en django_session. La auditoría de negocio no tiene UPDATE/DELETE concedidos a Django. La cuenta de migraciones se usa por separado.

## Pendientes fuera de esta migración

M21 requiere aún los consumos web restantes de la aplicación de usuarios; M22 conserva documentación/eventos y validación conjunta del alcance global. No se cierran esas tarjetas enteras por terminar solo la migración administrativa. Siguen pendientes servidor/dominio HTTPS, firma Android de entrega, teléfono físico/aceptación, revisor y plazo de retención. La falta de cuota IA comprobada en el corte anterior no se volvió a ensayar aquí. La base H2 histórica se conserva separada y no se migró todo su contenido.
