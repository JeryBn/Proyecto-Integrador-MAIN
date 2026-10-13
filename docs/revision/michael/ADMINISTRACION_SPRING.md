# Backend administrativo Spring Boot

Implementado por solicitud de Michael en main local, 13/10/2026. Sustituye el backend Django anterior. Operación mínima propuesta HU10: crear actividad en un curso existente. No implementa toda administración, comunicados, exámenes, gestión de alumnos ni rol docente restringido por curso. El cambio tecnológico respecto a T09 debe quedar acordado con equipo/profesor; no se modificó Trello ni su aceptación.

## Arranque en IntelliJ

Las clases AdminController, AdminService, AdminApiException y AdminErrorHandler se agrupan en `pe.tecsup.main.administracion`. La prueba AdminIntegrationTest está en el mismo package bajo src/test. SecurityConfig permanece en el package principal porque configura el acceso de alumno y administrador; AdminCredentialsTest permanece junto a esa configuración compartida.

JDK 21 o superior. No requiere Python ni otro servidor: todo corre en MainApplication, puerto 8088.

En Run > Edit Configurations > MainApplication > Environment variables, configurar `MAIN_ADMIN_USERNAME=admin` y `MAIN_ADMIN_PASSWORD` con una contraseña propia de al menos 12 caracteres. Se convierte a BCrypt al arrancar. No guardar la contraseña en archivos del repositorio ni en configuraciones compartidas del IDE. Alternativa: `MAIN_ADMIN_PASSWORD_HASH` con BCrypt previamente generado; no configurar password y hash juntos. Sin contraseña/hash la cuenta administrativa no existe; las demos de alumno siguen funcionando.

Para arrancar desde PowerShell (contraseña sin historial):

```powershell
$env:MAIN_ADMIN_USERNAME = 'admin'
$clave = Read-Host 'Contraseña administrativa (mínimo 12 caracteres)' -AsSecureString
$env:MAIN_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $clave).Password
mvn clean package
java -jar target/main-sprint1-0.1.0.jar
```

Detener antes la instancia anterior en 8088. El formulario de login existente es para alumno; Pamela construirá el acceso administrativo contra la API JSON/form indicada abajo. No se añadieron pantallas.

## Contrato para React

URL base `http://127.0.0.1:8088`. Rutas SIN barra final. Mismo origen o proxy React `/api` a Spring conservando cookies. Sin CORS abierto. Sesión JSESSIONID; no hay token JWT.

| Método / ruta | Entrada | Respuesta |
|---|---|---|
| GET /api/admin/csrf | Público | 200 `{parameterName,headerName,token}`; crea sesión |
| POST /api/admin/login | FORM urlencoded `username`, `password`, `_csrf` | 200 `{rol:"ADMIN"}`; 401 credenciales inválidas; 403 cuenta ALUMNO o CSRF inválido |
| POST /api/admin/logout | Token CSRF | 200 `{message:"Sesión finalizada"}`; invalida sesión |
| GET /api/admin/perfil | Sesión ADMIN | 200 `{usuario,rol:"ADMIN"}` |
| GET /api/admin/cursos?offset=0&limit=20 | Sesión ADMIN | 200 `{items:[{id,nombre,docente}],total,offset,limit}`; orden ID |
| GET /api/admin/actividades?offset=0&limit=20 | Sesión ADMIN | 200 `{items:[Actividad],total,offset,limit}`; orden fechaLimite,id |
| GET /api/admin/actividades/{id} | Sesión ADMIN | 200 Actividad; 404 inexistente |
| POST /api/admin/actividades | JSON, sesión ADMIN y CSRF | 201 Actividad, cabecera Location; 409 duplicado |

`offset`: 0..1000000; `limit`: 1..100. El listado administrativo incluye entregadas, pues administra actividades; el listado del alumno continúa excluyéndolas y filtrando por matrícula.

Flujo React:
1. GET csrf con `credentials:"include"`; guardar token.
2. POST login usando `URLSearchParams({username,password,_csrf:token})` y cookies. No enviar JSON al login.
3. GET csrf de nuevo: el token se rota al autenticar y el ID de sesión cambia.
4. Usar el nuevo token en cabecera `X-CSRF-TOKEN` para POST actividad/logout, con cookies.
5. 401: solicitar acceso; 403: mostrar rechazo de permisos o renovar CSRF según el contexto. No mostrar alumno como administrador.

Sesión compartida entre dominios del mismo servidor: cambiar a administrador reemplaza la identidad de alumno en ese navegador. ADMIN no puede consultar APIs del alumno; ALUMNO no puede administrar. Logout administrativo cierra esa sesión.

POST actividad:
```json
{"id":100,"cursoId":1,"titulo":"Nueva tarea","descripcion":"Instrucciones","fechaLimite":"2026-11-20T15:30:00"}
```

Todos los campos obligatorios. IDs enteros positivos JSON hasta 9007199254740991 (rango exacto de JavaScript); no se aceptan booleanos, fracciones ni texto numérico. `cursoId` existente. Título 1..150 y descripción 1..2000 caracteres tras eliminar espacios exteriores. Fecha local America/Lima exacta `YYYY-MM-DDTHH:mm:ss`, sin offset/Z, fecha válida. Se permiten fechas pasadas según reglas actuales de vencimiento. Campos desconocidos se rechazan. Renderizar texto como texto en React.

Respuesta Actividad:
```json
{"id":100,"cursoId":1,"curso":"Curso existente","docente":"Docente existente","titulo":"Nueva tarea","descripcion":"Instrucciones","fechaLimite":"2026-11-20T15:30:00"}
```

Errores de validación:
```json
{"error":{"code":"VALIDATION_ERROR","message":"Revisa los campos.","fields":{"fechaLimite":"Fecha Lima obligatoria: YYYY-MM-DDTHH:mm:ss, sin offset."}}}
```

400 datos/JSON/parámetros inválidos; 401 sesión ausente o login inválido; 403 rol incorrecto o CSRF; 404 inexistente; 405 método no implementado; 409 conflicto de integridad sin sobrescritura; 415 Content-Type incorrecto. No depender del cuerpo de 405/415/500: puede usar el formato genérico de Spring. CSRF se valida antes de autenticación en POST; un POST sin token puede responder 403 antes que 401. No hay edición ni borrado.

## Modelo y T10

Usa JdbcTemplate con SQL parametrizado sobre las tablas existentes `actividad`, `curso`, `docente`; no se crean tablas ni se cambian migraciones. Los IDs son BIGINT sin autoincremento: coordinar un ID libre, no hacer MAX(id)+1 en React. Para asignación automática se requiere migración acordada.

En H2, la nueva actividad aparece inmediatamente a los alumnos matriculados del mismo proceso. Al reiniciar H2 se pierde: no constituye persistencia. Para persistir, activar perfil mysql y configurar `MAIN_DB_URL`, `MAIN_DB_USER`, `MAIN_DB_PASSWORD`; el perfil ya usa `spring.sql.init.mode=never`. No volver a crear tablas existentes. Una sola aplicación usa la misma conexión para administración/alumno.

Las cuentas siguen siendo InMemoryUserDetailsManager: la cuenta ADMIN se reconstruye desde entorno, no desde la tabla docente. Identidad persistente, vinculación docente/curso y administración de usuarios quedan fuera de esta operación mínima.

## Pruebas

```powershell
mvn test
```

Pruebas nuevas: AdminIntegrationTest y AdminCredentialsTest. Incluyen creación→lectura del alumno, rechazo a otro alumno, validación, duplicados sin sobrescribir, paginación, permisos, login, fijación de sesión, rotación CSRF y logout. Se conservan las pruebas anteriores de HU06/HU07. Validación MySQL y persistencia tras reinicio pendientes de disponer de servidor compartido.

Antes de publicar un despliegue, configurar HTTPS, credenciales y control de intentos repetidos en el proxy según el acuerdo de despliegue. El presente incremento es backend local; no declara aceptación del profesor.
