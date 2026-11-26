# Plataforma escolar MAIN

La portada está en `/bienvenida.html`. Estudiantes, docentes y apoderados ingresan por `/login.html`; el servidor identifica el rol desde la cuenta. Administración permanece en Django.

## Preparación desde administración

1. Crear el año y sus trimestres o bimestres en Seguimiento escolar → Configuración.
2. Crear aulas con nivel, grado, sección y escala de calificación. Seleccionar el año y periodo de los filtros superiores y establecerlo como actual.
3. Crear cuentas de alumnos, docentes y familiares. Las cuentas familiares corresponden a los apoderados.
4. Verificar la identidad del apoderado y registrar el vínculo con su hijo en Parentesco verificado.
5. Matricular al alumno en un aula y asignar sus cursos a las cuentas docentes.
6. Registrar asistencia diaria. Un día sin registro no se considera falta.

Una cuenta familiar sin vínculos no ve alumnos. Si falta la matrícula escolar por año y aula, la plataforma explica qué debe registrar administración; no inventa notas ni estadísticas.

## Recorrido de evaluaciones

El docente entra a Evaluación escolar, selecciona aula, curso y periodo y registra una evaluación con competencia y nota numérica o logro AD/A/B/C. El alumno consulta sus propias evaluaciones y el apoderado autorizado ve las mismas en Notas y libreta. Puede filtrar curso y periodo y descargar el PDF. Las correcciones conservan la evaluación y rechazan versiones antiguas.

El PDF vuelve a comprobar el vínculo antes de generarse. Incluye estudiante, año, aula, periodo, curso, competencia, evaluación y nota/logro. Las libretas extensas se paginan. Conserva las valoraciones registradas y no convierte AD/A/B/C en números ni decide la promoción de grado.

Las notas numéricas escolares se reflejan en `course_grade`, para los lectores anteriores. Deben editarse en Evaluación escolar o Seguimiento escolar; las pantallas anteriores rechazan cambios sobre estas evaluaciones para evitar discrepancias. Las calificaciones antiguas mantienen sus vistas anteriores.

## Publicaciones

El docente publica comunicados, tareas y exámenes para sus cursos o un aula asignada. Las tareas y exámenes requieren fecha; el campo se destaca si falta. Un identificador de envío evita duplicar una publicación cuando se reintenta con los mismos datos.

Los alumnos consultan publicaciones de sus cursos. Los apoderados ven las del hijo vinculado. Leer publicación confirma la lectura; listar títulos no la confirma. El docente consulta las lecturas confirmadas y el número de destinatarios. Revocar un vínculo impide nuevos accesos y elimina esa familia de la audiencia vigente.

## Correspondencia con el backlog

| Historia | Función |
| --- | --- |
| HU-01 | Notas del apoderado por curso, competencia y periodo; periodo actual configurado por administración. |
| HU-02 | Descarga de libreta PDF del estudiante autorizado con los filtros elegidos. |
| HU-03 | Selección de año, curso y trimestre. |
| HU-04 | Asistencia diaria y filtros por fecha. |
| HU-05 a HU-08 | Se conservan horario, pendientes, detalle y agenda del alumno. |
| HU-09 | Comunicados para las familias del curso o aula. |
| HU-10 | Tareas con instrucciones y fecha obligatoria. |
| HU-11 | Exámenes con fecha e indicaciones. |
| HU-12 | Confirmación de lectura del apoderado y consulta docente. |

La aceptación del profesor o equipo requiere comprobar estos recorridos con sus cuentas y criterios del backlog. Las pruebas técnicas no sustituyen esa aceptación.

## Ejecución y comprobaciones

Recargar Maven en IntelliJ, reiniciar Usuarios MAIN - MariaDB (8088), reiniciar Administración Django (8090) y actualizar el navegador. La portada local es `http://127.0.0.1:8088/bienvenida.html`. Ambos servidores deben consultar la misma MariaDB; `.env.local` guarda la conexión privada y está excluido de Git.

Flyway administra el esquema. V21 agrega competencias y periodo actual; V22 publicaciones y lecturas; V23 establece tema oscuro para cuentas nuevas sin cambiar preferencias existentes; V24 permite fechas escolares posteriores a 2038 en MySQL/MariaDB. PDFBox 3.0.8 genera las libretas.

Usar Java 21, la versión del proyecto, también para las pruebas:

- Java: `mvn -Dspring.profiles.active=test test`. El perfil test usa H2 y credenciales de pruebas.
- Django: `python manage.py test administration --settings=main_admin.test_settings --noinput`, desde `admin-django`.
- Web: `node --test test/*.test.mjs` y `node build.mjs`, desde `frontend`.

`FamilySchoolIntegrationTest` cubre vínculo revocado, accesos cruzados, periodos ajenos, notas numéricas y literales, competencias, correcciones, PDF paginado, asistencia y lecturas. La ampliación corresponde a la web; dispositivo Android, Firebase/FCM, publicación y cuotas de IA mantienen su evaluación independiente.
