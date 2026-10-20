# Revisión del 20 de octubre de 2026

Informe factual preparado para Jery Becerra Ninaquispe. Los documentos Word y PDF contienen el mismo informe de 10 páginas, revisado visualmente. La matriz identifica responsables originales y pendientes; no atribuye ausencia total de trabajo a ningún integrante.

## Cambios y pruebas

- `2e5684d`: administración web de cuentas y vínculos; cierre de sesión entre pestañas corregido.
- `54b1168`: web de cursos, horarios y notas; registro/edición docente y consulta privada del alumno, con control de concurrencia.
- Dos ejecuciones del servidor el 20/10: 80 casos por ejecución, sin fallos, errores ni omitidos. No se suman las repeticiones como casos distintos. Resumen por clase en `server-tests.json`.
- Navegador: panel cargado; cierre normal y con renovación de sesión en otra pestaña; creación de nota sintética 18/20, edición 19/20 y lectura del alumno 19/20.
- Tras reiniciar el paquete final se comprobó persistencia de la nota y apertura docente de Cursos y notas sin consultar actividades exclusivas del alumno.
- React compilado. Android Studio, servidor y emulador abiertos; APK existente reinstalado. Las pruebas Android citadas del 18/10 son históricas.

## Trello

M03 y M07 se movieron a FINALIZADO y se marcaron completas con comentario de evidencia. Se añadieron avances y límites a M01, M02, M04–M06, M08–M24 y T11. Las otras cinco tarjetas previamente finalizadas conservan ese estado. La aceptación del profesor y del equipo no se presume.

## Pendientes

Consultar `../../REVISION_2026_10_20.md` y `matriz-tareas.json`. No están implementados los adjuntos de chats ni la paridad web completa, entre otros requisitos. No hay despliegue de producción ni APK firmado de entrega. La última comprobación real de OpenAI quedó sin saldo. No se incluye ningún secreto en estos documentos.

## Prueba manual

Servidor local: http://localhost:8088. Portal académico: `/`; administración: `/admin.html`; revisión humana: `/moderation.html`.

Usar únicamente cuentas de demostración del entorno local. Los docentes ven sus cursos; en un curso pueden elegir un estudiante, registrar una nota o editar una existente. El alumno matriculado ve el cambio en Cursos y notas tanto desde web como desde Android. Una nota editada por otra sesión exige actualizar antes de guardar.

La firma de Android, hosting, dominio, revisor de apelaciones y conservación todavía requieren decisiones externas. El desarrollo que falta sigue identificado como trabajo pendiente y no como bloqueo externo.
