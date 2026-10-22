# Proyecto Integrador MAIN - seccion A

## Actualización del 22 de octubre de 2026

[Migración de la administración restante a Django](docs/MIGRACION_DJANGO_22_OCT.md): cuentas, parentescos, contactos/reportes, cursos/matrículas, horarios, notas, auditoría y moderación con adjuntos operan directamente sobre MySQL compartido. Spring mantiene usuarios y Android; el iniciador cierra su API administrativa histórica. Verificados 104 casos de servidor, 32 Django y recorridos HTTP reales por módulo. La aceptación del equipo, paridad web completa y despliegue público siguen pendientes.

## Corte histórico del 21 de octubre de 2026

[Regularización de las tareas del 12 de octubre](docs/REGULARIZACION_SPRINT_12_OCT.md): Django administrativo y React conectados a MySQL compartido con Spring Boot, según el diagrama confirmado por Jery. Se conservan los aportes anteriores y su autoría. El entorno local de usuarios está en 8088 y el administrativo Django en 8090. La base H2 previa se preservó; no se afirma migración de todos sus ensayos.

[Informe detallado en Word](docs/informes/2026-10-21-sprint12/MAIN_Regularizacion_Sprint_12_octubre_2026.docx) y [PDF de seis páginas](docs/informes/2026-10-21-sprint12/MAIN_Regularizacion_Sprint_12_octubre_2026.pdf), con responsables originales, brechas cubiertas, resultados y pendientes. T03–T06 cerradas técnicamente en Trello por ejecución asistida; T09/T10/T11 conservan pendientes los acuerdos oficiales indicados en las tarjetas.

Verificados: 99 casos de servidor, 15 Django, 4 de formulario web, contrato HTTP cruzado con persistencia tras reiniciar y 8 casos Android sobre MySQL (3 de arquitectura y 5 de notas/estados/adjuntos). Cada suite y su límite están descritos en [las evidencias](docs/informes/2026-10-21-sprint12/LEEME.md). Django cubre las operaciones administrativas del Sprint 1; los módulos administrativos posteriores aún requieren migración/paridad. [Preparación Django](admin-django/README.md).

## Corte histórico del 20 de octubre de 2026

Proyecto académico en desarrollo con Spring Boot, React y Android. Se conservan los aportes de Michael Julian Fernandez Camarena (API administrativa `b386291`) y Pamela Regina Meneses Mayhua (panel React `a34efa6`). Los incrementos posteriores se realizaron con asistencia técnica por encargo de Jery Becerra Ninaquispe; no se presume aceptación del equipo ni proyecto completo.

- [Estado y pendientes por tarjeta](docs/REVISION_2026_10_20.md).
- [Informe detallado en Word](docs/informes/2026-10-20/MAIN_Informe_de_avances_20_octubre_2026.docx) y [PDF](docs/informes/2026-10-20/MAIN_Informe_de_avances_20_octubre_2026.pdf).
- [Pruebas y evidencias del 20 de octubre](docs/informes/2026-10-20/LEEME.md).
- Administración web de cuentas, vínculos, cursos y matrículas con auditoría; notas del alumno y registro/edición docente añadidos el 20/10. Android incluye comunicación privada, grupos y estados con revisión humana previa. 96 pruebas de servidor y siete recorridos Android del 20/10 aprobados; no equivale a cobertura completa. [Contrato y evidencias de administración](docs/ADMINISTRACION_ACADEMICA_2026_10_20.md).
- Pendientes principales: formatos de documentos adicionales y grabadora de voz, notificaciones en segundo plano, paridad web restante, moderación automática/operación, despliegue y firma Android. La última llamada real de OpenAI quedó sin saldo; la integración no se presenta como respuesta generada verificada.

Los avances anteriores y los nuevos commits están integrados en `main` por instrucción de Jery. Los adjuntos privados, la revisión web y los visores Android están publicados y verificados. [Contrato de formatos y pruebas](docs/ADJUNTOS_PRIVADOS_2026_10_20.md).

## Información histórica del primer incremento

Las instrucciones y paquetes siguientes corresponden a la base HU06/HU07. Para conocer las funciones actuales y sus límites, usar la revisión enlazada arriba. El ZIP histórico no contiene los incrementos nuevos.

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
Bases del profesor: administracion React + Django; usuario React/Kotlin + Spring Boot y base compartida. Este avance incorpora React + Spring Boot y el cliente Kotlin de HU06/HU07 en mobile/. Django, usuarios persistidos, administracion, roles adicionales, IA y despliegue siguen pendientes. La validacion en telefono fisico y la aceptacion del equipo continuan pendientes. Las estimaciones 40/49,5/56 no prueban acuerdo del equipo; validar alcance y aceptacion con profesor e integrantes.

## Aplicación Android de Jery

Abrir [mobile/](mobile/README.md) en Android Studio con Gradle JDK 21. Incluye acceso por sesión/CSRF, listado autorizado, búsqueda/filtros, detalle, regreso con contexto, estados de error/vacío y cierre de sesión. Conexión al backend local mediante `adb reverse tcp:8088 tcp:8088`; no necesita modificar su dirección de escucha.

Plan y responsabilidades: [docs/mobile/PLAN_Y_CRITERIOS.md](docs/mobile/PLAN_Y_CRITERIOS.md). Resultados por tarea y capturas en [docs/mobile/](docs/mobile/). Desarrollo asistido de Jery en la rama `jery/app-kotlin-hu06-hu07`, conservando la autoría de React y el ámbito de Django. Las tareas compartidas requieren revisión del equipo; la evidencia Android corresponde al emulador.

## Colaboracion
Repositorio publico. Para subir cambios se debe aceptar la invitacion. Ramas: jery/backend-arquitectura, pamela/interfaz-criterios, michael/pruebas-seguridad. Ver CONTRIBUTING.md. La base se preparo por solicitud de Jery con asistencia registrada en PROMPTS.md; no atribuye trabajo a integrantes ausentes. Cada integrante registra modificaciones reales y abre PR.

## Fuentes
- Bases: https://docs.google.com/presentation/d/1LU-aAC9IeDRlVXaWTJFTgHlsXwUBkYal/edit
- Canvas: https://tecsup.instructure.com/courses/74457/modules
- Spring Boot: https://docs.spring.io/spring-boot/3.4/reference/testing/spring-boot-applications.html
- Spring Security: https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html

## Backend administrativo Spring Boot

Administración Java en el mismo servidor: crear actividades, consultar cursos/listado/detalle y acceso por rol ADMIN. Instalación y contrato para React: [docs/revision/michael/ADMINISTRACION_SPRING.md](docs/revision/michael/ADMINISTRACION_SPRING.md). Configurar credenciales administrativas por entorno. H2 permite probar el flujo administración→alumno; persistencia MySQL sigue pendiente de T10. Spring sustituye la propuesta Django por instrucción de Michael; actualizar el acuerdo tecnológico en Trello con el equipo. HU10 permanece propuesta para aceptación.
