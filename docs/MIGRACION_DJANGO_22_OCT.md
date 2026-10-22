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

## Pendiente en esta migración

Moderación de mensajes/estados con descarga segura de adjuntos. No se declara terminada M22 ni la paridad global M21.

## Incremento: horarios y notas administrativas

- Django registra/retira horarios y registra/edita notas por curso. Conserva el bloqueo por curso/docente, detección de cruces, matrícula requerida, escala de hasta 1000 y dos decimales, versión temporal de nota y auditoría atómica.
- Las rutas docentes y estudiantiles permanecen en Spring.
- 27 pruebas Django aprobadas y 15 verificaciones HTTP reales sobre MySQL: horario compartido, cruce rechazado, nota creada/editada, versión obsoleta rechazada y nota oculta tras revocar matrícula.
- Evidencia `docs/informes/2026-10-22-django/scheduling-grades-http.json`. Conserva curso y nota sintéticos; retira horario y matrícula de esa prueba.
- La primera ejecución detectó un permiso insuficiente de bloqueo de `docente` en MySQL; se otorgó únicamente UPDATE de la columna id en esa tabla del esquema aislado y se repitió el recorrido completo correctamente. Una primera matrícula sintética requiere limpieza identificada; no afecta datos académicos reales.
