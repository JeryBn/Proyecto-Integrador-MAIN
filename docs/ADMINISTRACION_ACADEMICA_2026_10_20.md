# Administración académica y auditoría

Incremento `b99080e`, realizado con asistencia técnica por encargo de Jery. Conserva la API de actividades de Michael y el panel original de Pamela. `/admin.html` incorpora **Cursos y matrículas**, además de **Cuentas y vínculos** y acceso a revisión humana.

## Contrato

Todas las rutas siguientes requieren cuenta ADMIN persistida y activa; las mutaciones requieren CSRF. Los alumnos, docentes y familiares no pueden otorgarse acceso ni consultar la auditoría.

| Ruta | Uso |
|---|---|
| GET `/api/admin/v1/courses?offset=0&limit=20` | Cursos con `id`, `name`, `teacherId`, `teacher` y `version` |
| POST `/api/admin/v1/courses` | Crear con `{id,name,teacherUsername}`; docente activo y nombre de hasta 100 caracteres |
| PUT `/api/admin/v1/courses/{id}` | Editar con esos campos y `expectedVersion`; conflicto 409 si otra sesión editó el curso |
| GET `/api/admin/v1/courses/{id}/students?offset=0&limit=20` | Estudiantes matriculados; incluye usuario, identificador académico y nombre |
| PUT `/api/admin/v1/courses/{id}/students` | `{username,enrolled:true}` matricula; `false` revoca; requiere alumno activo para nueva matrícula |
| GET `/api/admin/v1/audit?offset=0&limit=20` | Eventos administrativos; actor, acción, identificador del objeto y fecha |

Las páginas admiten `offset` de 0 a 1000000 y `limit` de 1 a 100. No existe borrado de cursos ni de historial académico en este incremento. Los errores usan el contrato v1 `error.code` y `error.message`. Duplicar identificadores provoca 409 sin cambiar datos; los intentos sin permiso se rechazan.

## Acceso y concurrencia

- La matrícula afecta inmediatamente a las consultas posteriores de agenda, actividades y notas. Su revocación conserva notas y entregas históricas y retira su lectura académica al alumno; matricularlo nuevamente restaura ese acceso.
- La matrícula no elimina contactos ni grupos: su autorización es independiente y se controla por sus contratos propios.
- La asignación docente se comprueba en cada consulta de agenda y de notas. Reasignar el curso revoca el acceso del docente anterior. No se permite reasignar un curso con clases registradas antes de ajustar o retirar su horario, para evitar solapamientos no revisados.
- Un bloqueo de curso serializa cambios de matrícula, registro de notas, horario y edición administrativa; `expectedVersion` evita sobrescribir una edición concurrente del curso.
- Los contactos y parentescos continúan bajo autorización administrativa. Los alumnos no pueden reclamar la identidad académica de otra cuenta.

## Auditoría y migración

Flyway V8 agrega `administrative_audit` y `curso.admin_version`; no elimina tablas ni datos existentes. Se respaldó la base local sintética antes de aplicarla y se comprobó el arranque sobre V7 con una sola migración nueva. MySQL real sigue pendiente de M16.

Los eventos nuevos registran creación/deshabilitación de cuentas y restablecimiento de acceso, verificación/revocación familiar, autorización de contactos, cursos, matrículas, horarios, notas, administración de grupos y decisiones de revisión de mensajes y estados. Se guardan dentro de la misma transacción. No se registran contraseñas, tokens, claves, cuerpos de mensajes ni notas numéricas en este registro. Los cambios anteriores a V8 no se reconstruyen ni se atribuyen retrospectivamente. No hay endpoint para editar o borrar eventos. La política de conservación institucional aún debe acordarse.

## Evidencia del 20 de octubre

- 88 pruebas de servidor aprobadas: ocho nuevas de administración y la regresión existente, sin fallos, errores ni omitidas.
- Web: curso 9100 creado y editado; estudiante `alumno` matriculado; listado y auditoría consultados.
- Android: `AcademicAdministrationUiTest` aprobada. El curso de prueba 9200 aparece y al revocar matrícula desaparece de la consulta académica. La captura incluye también el curso 9100 creado desde React.
- La primera ejecución móvil tuvo un selector ambiguo porque un curso aparece tanto en notas como en agenda. Se corrigió el selector y se repitió con éxito; no se ocultó un fallo del producto.
- Línea base local independiente: `scripts/verificar_carga_local.py`, ocho sesiones y 160 lecturas autenticadas, cero errores, mediana 9,29 ms y p95 14,54 ms. Excluye latencia de login de los percentiles. Es una medición sobre datos sintéticos en loopback con IDE/emulador abiertos; no demuestra capacidad de producción.

La finalización técnica de M13 no sustituye aceptación del profesor ni del equipo. El despliegue, firma, moderación automática, retención y apelaciones conservan sus tarjetas específicas.
