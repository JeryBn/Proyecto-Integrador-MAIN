# Arquitectura web COLEGIA

## Inspección previa y orden aplicado

La copia `fronted/web` tenía cambios locales y archivos nuevos; se respaldaron antes de mover. Un solo build esbuild generaba usuario y administración y copiaba ambos a los estáticos de Spring. Django servía los mismos archivos mediante rutas propias. La API usa sesión, cookies, CSRF, BCrypt y JdbcTemplate. El perfil base usa H2; mysql usa variables MAIN_DB_*. La copia privada previa apuntaba a MariaDB 11.4.10 en 3312. La propuesta Compose usa MySQL 8.0 en una base distinta.

Orden: respaldo y registro Git; traslado de backends; separación de dos proyectos Vite; proxies y rutas de acceso; Compose y seed explícito; documentación y compilaciones/pruebas. Se conservaron todos los cambios previos, los nombres técnicos y el historial de migraciones. No se hicieron commits ni pushes.

## Aplicaciones

- React Usuario llama a Spring: actividades, horario, notas, asistencia familiar, libreta PDF, publicaciones, mensajes y asistente.
- React Admin llama a Django: cuentas, vínculos, cursos, aulas, años, periodos, matrículas y seguimiento.
- Spring ejecuta Flyway sobre el esquema compartido. Django espera su disponibilidad y ejecuta `migrate` solo para tablas internas.
- Ollama es opcional. El adaptador añadido usa `/api/chat`, `stream=false`, URL y modelo configurables. Esta copia tenía un proveedor OpenAI; se preservó como opción anterior, sin confundirlo con una integración Ollama previamente existente.

## Sesiones, permisos y CSRF

Usuario obtiene `/login-token`, hace POST `/login` y conserva la cookie de sesión. Después consulta `/api/perfil`. Logout POST `/logout` requiere token. Administración usa `/api/admin/csrf`, `/api/admin/login`, `/api/admin/perfil` y `/api/admin/logout`, con cookie MAIN_ADMIN_SESSION.

Vite en desarrollo y Nginx en Compose mantienen Host y puerto del navegador al reenviar al backend. Las rutas son relativas; cada frontend habla con su propio backend. No se habilita CORS global. Si se elimina el proxy, hay que declarar orígenes concretos con credenciales en Spring y configurar CSRF_TRUSTED_ORIGINS en Django, además de revisar SameSite/HTTPS. Esa variante no es la ejecutada por esta entrega.

ALUMNO, DOCENTE y FAMILIAR entran al portal Usuario. ADMIN y REVIEWER usan Django. Ocultar un menú no reemplaza autorización. Los servicios verifican cuenta activa, curso, matrícula y vínculo. La verificación documental del parentesco sigue siendo manual.

## Compatibilidad

Se conservan `pe.tecsup.main`, artefacto main-sprint1, variables MAIN_*, X-MAIN-Expected-User, tablas e identificadores. La carpeta del worktree permanece con su nombre registrado. El build no modifica las migraciones aplicadas. Los scripts antiguos de OCI y ejecución local conservan su finalidad histórica; el arranque web recomendado es el Compose de la raíz.

## Brechas funcionales conservadas

Coexisten `matricula`/`school_enrollment`, `course_grade`/`school_mark`, docente por curso/asignación y `actividad`/`school_publication`. Las tareas nuevas no se insertan en `actividad`; HU06/HU07 siguen operando con actividades anteriores. El promedio gráfico es orientativo; reglas oficiales de competencias, cierre y promoción requieren definición académica.
