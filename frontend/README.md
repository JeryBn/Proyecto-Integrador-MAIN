# Web React de MAIN

Desde esta carpeta: npm ci y npm run build. El script genera dist/, copia assets al backend y actualiza su index.html. Luego ejecutar mvn package en la raíz del proyecto y copiar el JAR generado a la raíz antes de usar INICIAR-MAIN.cmd.

La web se sirve desde el mismo origen del backend; no necesita un segundo servidor en la demostración. App gestiona estado de búsqueda/filtros, ActivityCard representa tarjetas y ActivityDetail solicita un detalle autorizado. El formulario auxiliar de login continúa siendo HTML con protección CSRF.

Los datos provienen de la API, no de un listado fijo incrustado en React. El control de acceso se aplica en Spring Boot.

## Por dónde empezar

- `src/alumno/main.jsx` abre el portal de usuarios. Busca `ActivityCard` para ver cómo se muestra una actividad.
- `src/administracion/admin.jsx` abre el panel administrativo.

Una actividad puede llegar desde el servidor como JSON. Este ejemplo muestra solo los campos que usa la tarjeta:

```json
{
  "id": 123,
  "titulo": "Práctica de React",
  "curso": "Construcción y Pruebas de Software",
  "fechaLimite": "2026-12-01T12:00:00",
  "vencida": false
}
```

React recibe esos datos y muestra «Pendiente» cuando `vencida` es `false`.

## Recorrido para explicar el proyecto

1. En `src/administracion/admin.jsx`, busca `crearActividad`. El formulario junta los datos, los envía en JSON y abre el detalle cuando la API confirma que se guardaron.
2. En `src/alumno/main.jsx`, busca `ActivityCard`. La tarjeta muestra los datos de la actividad y decide con un `if` si está pendiente o vencida.
3. En `src/administracion/gestion-academica/academic-admin.jsx`, busca `guardarCurso` y `matricularEstudiante` para explicar qué pasa al pulsar esos botones.


| Panel | Carpetas por función |
| --- | --- |
| Administración | `gestion-academica` (cursos y matrículas), `cuentas` y `revision`. El formulario, listado y detalle de actividades viven en `admin.jsx`. |
| Alumno | `academico` (información académica y recordatorios), `comunicacion` (chats, estados, avisos y medios), `perfil`, `asistente` y `apelaciones`. |

Cada panel tiene sus propios estilos. En `alumno/datos` están el cliente de la API y las funciones que el portal usa para consultar datos.

En el proyecto original, el navegador carga React y las solicitudes de administración van a Django (8090). Las del alumno van a Spring Boot (8088). Por ejemplo, Django guarda una actividad en MariaDB y Spring Boot consulta esa misma base para mostrar las actividades que corresponden al alumno.
