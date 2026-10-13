# Administración Spring Boot — resultados

13/10/2026, America/Lima. Implementación asistida solicitada por Michael en main local. Spring Boot reemplaza el aporte Django por instrucción explícita del usuario. Confirmación del cambio tecnológico y alcance HU10 en Trello pendientes del equipo.

## Entrega

API /api/admin: sesión administrativa, CSRF, perfil, cursos paginados, actividades paginadas, detalle y creación de actividades con validación. Usa misma conexión JDBC/tablas de Spring; no cambia esquema ni crea una base aparte. Rol ADMIN configurado por entorno y BCrypt; cuenta deshabilitada si no se configura. No se modificó el frontend React.

## Verificación

JDK 22.0.2, release Java 21. Maven package: BUILD SUCCESS. 29 pruebas: 0 fallos, 0 errores, 0 omitidas (14 existentes, 12 integración administrativa, 3 credenciales).

Prueba integrada: ADMIN crea actividad de curso 1; alumno matriculado la consulta por detalle y listado; otro alumno recibe 404 y no la ve en pendientes. Se verifica contra la misma base H2 del contexto de integración; cada prueba revierte sus escrituras.

También se verificaron campos/fechas/tipos, curso inexistente, ID duplicado sin sobrescritura, acceso anónimo/no administrativo, JSON incorrecto, paginación, métodos no soportados, credenciales, CSRF, rotación de sesión/token y cierre de sesión. Las pruebas previas de login/listado/detalle del alumno continúan pasando.

Comando usado en el entorno restringido: Maven package con caché local del workspace y argLine de javaagent ByteBuddy para evitar el problema de autoattach del entorno. Fuera de ese entorno ejecutar `mvn test` o `mvn clean package` con JDK 21+.

## Límites pendientes

H2 reinicia sus datos; no se probó persistencia MySQL tras reinicio. T10 requiere configurar perfil mysql y una base acordada; no se modificaron tablas compartidas. IDs manuales porque el esquema no genera IDs. Cuenta administrativa en memoria por entorno; identidad persistente y docentes restringidos por curso fuera de este incremento. Contrato/arranque: ADMINISTRACION_SPRING.md. Evidencia de ejecución: SPRING_ADMIN_TEST_LOG.txt. No se publicó ni se modificó Trello.
