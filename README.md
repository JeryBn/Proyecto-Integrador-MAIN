# Proyecto Integrador MAIN — avance de Jery, sección A

Primer incremento de backend para HU06 (actividades pendientes) y HU07 (detalle autorizado). Este repositorio publica exclusivamente arquitectura, modelo de datos y backend. La interfaz React de Pamela y nuevas pruebas de Michael están pendientes de sus aportes e integración. No representa el proyecto final terminado.

## Ejecutar en IntelliJ IDEA
Abrir esta carpeta como proyecto Maven y seleccionar JDK 21. Ejecutar MainApplication. Perfil predeterminado: H2 en memoria para pruebas y demostración; puerto 8088. Si la demo completa anterior sigue usando 8088, usar `--server.port=8089`.

Para la base persistente local, ejecutar primero database/01_CREAR_BASE.sql una sola vez en una base nueva, luego activar el perfil `mysql`. No volver a ejecutar el script de creación en una base ya creada; no incluye borrados ni reemplazos. Configurar MAIN_DB_URL, MAIN_DB_USER y MAIN_DB_PASSWORD mediante variables de entorno. Los valores predeterminados son exclusivamente para el XAMPP local de esta PC; no usar root sin contraseña en un servidor compartido.

```text
mvn test
mvn package
java -jar target/main-sprint1-0.1.0.jar --spring.profiles.active=mysql --server.port=8089
```

El backend aislado utiliza el formulario de login generado por Spring Security en /login y redirige a /api/perfil. Las cuentas sintéticas alumno, otro y vacio usan MainDemo2026!. La interfaz de Pamela deberá integrar el contrato de login y CSRF al añadir su parte.

## Implementado y comprobable
- Identidad tomada del Principal del servidor; consultas SQL parametrizadas.
- Pendientes limitados a los cursos de matrícula, excluyendo entregas existentes.
- Detalle ajeno o inexistente responde 404; solicitud API sin sesión responde 401.
- Modelo de alumno, docente, curso, matrícula, actividad y entrega.
- Sesión, rol ALUMNO, contraseñas BCrypt y CSRF.
- Clock inyectable con zona America/Lima. Al instante exacto del límite la actividad aún no está vencida; después sí, si no tiene entrega. Esta regla técnica queda propuesta para validación del equipo.
- Perfil H2 de pruebas y perfil JDBC MySQL para demostración persistente.

## API
GET /api/perfil; GET /api/actividades/pendientes; GET /api/actividades/{id}; GET /api/csrf. POST /login y /logout con CSRF. Endpoints de lectura no reciben una identidad de alumno elegida por el cliente.

## Arquitectura y límites
Bases del profesor: administración React + Django; usuario React/Kotlin + Spring Boot; base compartida. Este avance cubre Spring Boot y el modelo relacional. La conexión con Django y Kotlin, usuarios persistidos, administración, roles adicionales, IA y despliegue están pendientes. XAMPP incluye MariaDB, compatible con el perfil JDBC usado, y MySQL Workbench actúa como cliente; no confundir el cliente con el motor del servidor.

Las estimaciones 40/49,5/56 corresponden a registros distintos y no prueban un acuerdo del equipo. Se mantiene el alcance técnico HU06/HU07 para este avance, pendiente de validar capacidad y aceptación con los integrantes y el profesor. No se simula esa aceptación.

## Colaboración y procedencia
La base fue preparada por solicitud de Jery con asistencia registrada en PROMPTS.md. Las pruebas incluidas son validación de esta base, no una contribución ya realizada por Michael. Cada integrante añadirá cambios reales en su rama mediante Pull Request; no volver a subir la base como si fuera una nueva autoría.

Pamela: rama pamela/interfaz-criterios. Michael: michael/pruebas-seguridad. Jery: jery/backend-arquitectura. Ver CONTRIBUTING.md. El repositorio es privado: se requieren invitaciones a las cuentas exactas de los compañeros antes de que puedan subir cambios.

## Fuentes
- Bases del curso: https://docs.google.com/presentation/d/1LU-aAC9IeDRlVXaWTJFTgHlsXwUBkYal/edit
- Canvas: https://tecsup.instructure.com/courses/74457/modules
- Spring Boot: https://docs.spring.io/spring-boot/3.4/reference/testing/spring-boot-applications.html
- Spring Security: https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html
