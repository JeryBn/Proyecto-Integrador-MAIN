# Proyecto Integrador MAIN - seccion A

Base ejecutable HU06/HU07: interfaz React, backend Spring Boot y datos sinteticos H2. Integra la interfaz de referencia para facilitar el trabajo local; las modificaciones propias de Pamela y las nuevas pruebas de Michael siguen pendientes de sus aportes. No es el proyecto final terminado.

## Descargar el paquete completo
[Descargar MAIN_LOCAL_PAMELA.zip](entregables/MAIN_LOCAL_PAMELA.zip?raw=true). Abre el archivo y pulsa Download raw file si GitHub muestra su pagina. Extrae todo y ejecuta INICIAR-MAIN.cmd. Necesita Java JDK 21 o superior. No requiere Node, Maven o MySQL para esta demostracion.

Lee [LEEME-LOCAL.md](LEEME-LOCAL.md) para ejecutar, modificar y resolver errores. Abre http://127.0.0.1:8088/login.html cuando el servidor indique Started MainApplication. Cuentas sinteticas: alumno, otro y vacio; contraseña MainDemo2026!. Login en /login.html, POST /login, redireccion a /; cierre de sesion POST /logout con CSRF.

## Desarrollo
Abre el proyecto Maven en IntelliJ con JDK 21. Desde frontend: npm ci y npm run build. Desde la raiz: mvn clean package. Para usar el iniciador copia target/main-sprint1-0.1.0.jar a la raiz y reinicia. El ZIP contiene ese JAR ya compilado y todo el codigo fuente. El build de React genera index.html y copia los assets al backend.

Para persistencia ejecuta database/01_CREAR_BASE.sql una sola vez en una base nueva y activa el perfil mysql. Configura MAIN_DB_URL, MAIN_DB_USER y MAIN_DB_PASSWORD. Los valores por defecto son para XAMPP local; no usar root sin contraseña en servidor compartido. El motor XAMPP es MariaDB; Workbench es el cliente.

## Implementado
Principal del servidor, SQL parametrizado, pendientes limitados a matriculas sin entregas, detalle ajeno o inexistente 404, API sin sesion 401, rol ALUMNO, BCrypt, sesion y CSRF. Clock America/Lima: al limite exacto aun no vence; despues si, salvo actividad entregada. Esta regla queda propuesta para validacion del equipo.

API GET /api/perfil, /api/actividades/pendientes, /api/actividades/{id}, /api/csrf. Modelo: alumno, docente, curso, matricula, actividad y entrega.

## Arquitectura y alcance pendiente
Bases del profesor: administracion React + Django; usuario React/Kotlin + Spring Boot y base compartida. Este avance incorpora React + Spring Boot; Django, Kotlin, usuarios persistidos, administracion, roles adicionales, IA y despliegue siguen pendientes. Las estimaciones 40/49,5/56 no prueban acuerdo del equipo; validar alcance y aceptacion con profesor e integrantes.

## Colaboracion
Repositorio publico. Para subir cambios se debe aceptar la invitacion. Ramas: jery/backend-arquitectura, pamela/interfaz-criterios, michael/pruebas-seguridad. Ver CONTRIBUTING.md. La base se preparo por solicitud de Jery con asistencia registrada en PROMPTS.md; no atribuye trabajo a integrantes ausentes. Cada integrante registra modificaciones reales y abre PR.

## Fuentes
- Bases: https://docs.google.com/presentation/d/1LU-aAC9IeDRlVXaWTJFTgHlsXwUBkYal/edit
- Canvas: https://tecsup.instructure.com/courses/74457/modules
- Spring Boot: https://docs.spring.io/spring-boot/3.4/reference/testing/spring-boot-applications.html
- Spring Security: https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html
