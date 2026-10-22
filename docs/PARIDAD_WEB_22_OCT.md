# Paridad de la web de usuarios con MAIN Android

Trabajo asistido por indicación de Jery Becerra Ninaquispe. M21 conserva su asignación original a Pamela Regina Meneses Mayhua y el React existente. La administración Django ya migrada se mantiene separada; los clientes de usuarios consumen Spring y la misma base MySQL.

## Matriz de implementación y verificación

| Módulo | Android y API existentes | Web en este incremento |
| --- | --- | --- |
| Acceso, actividades, agenda y notas | Implementados; evidencia histórica y 3 Android/MySQL del corte anterior | Conservados; inicio agrupa tareas y cursos/notas; regresión visual final pendiente |
| Perfil, ajustes y contactos | Preferencias y contactos en API v1 | Implementados: tema, avisos, audiencia, bloqueo/silencio y reportes; pruebas de cliente correctas; recorrido real final pendiente |
| Avisos | Lista y lectura paginadas por permisos | Implementados con actualización visible y paginación; recorrido real final pendiente |
| Privados y grupos | Conversaciones, invitaciones, roles, revisión y mensajes v1 | Implementados: búsqueda, privados, grupos, invitaciones, miembros, roles, salida, edición/corrección/cancelación; recorrido conjunto final pendiente |
| Adjuntos | Imagen, video, audio y TXT; revisión previa | Interfaz implementada con subida, progreso, cancelación de espera, reintento idempotente y visor autorizado; recorrido conjunto final pendiente |
| Estados | Texto/multimedia; audiencia y caducidad; familiar no publica | Pendiente de interfaz web |
| Asistente | Servicio existente y clave autorizada; cuota agotada en corte previo | Pendiente de interfaz y comprobación real del proveedor |

## Primer incremento de perfil y sesión

- Cliente común usa credenciales del mismo origen, respuestas privadas sin caché, CSRF renovado en cada escritura y errores estructurados. No reintenta automáticamente una escritura de resultado incierto.
- `X-MAIN-Expected-User` protege la cuenta mostrada. Si otra pestaña cambia la sesión, Spring rechaza lectura/escritura con 409 `ACCOUNT_CHANGED` antes del controlador y el cliente descarta el estado de la página. Clientes Android anteriores que no envían esta cabecera siguen compatibles.
- El filtro se ejecuta antes del cierre de sesión; las preferencias y notas web comparten el cliente y no pueden escribir un borrador usando accidentalmente otra identidad.
- Tema y preferencias persisten en servidor, sin guardar información académica ni credenciales en almacenamiento del navegador. Las consultas periódicas se detienen cuando la pestaña está oculta y se actualizan al volver.
- Verificación técnica: React compilado, 11 pruebas de cliente (4 de acceso existentes y 7 nuevas) y 105 Maven aprobadas. No se declara paridad global ni aceptación con estos resultados parciales.

## Incremento de chats y archivos

- Chats privados desde contactos autorizados, búsqueda y filtros, creación de grupos, aceptar/rechazar invitación, gestión de miembros/administradores y salida conservan las reglas de la API. El último administrador no puede abandonar sin reemplazo.
- Listados y mensajes paginados, avisos enlazados a conversación, actualización cada cinco segundos mientras la pestaña está visible. Borradores por conversación se conservan en memoria al navegar entre secciones; nunca se guardan en disco.
- Editar/corregir vuelve a revisión; motivos y borradores solo aparecen al emisor según el contrato. Receptor conserva la versión aprobada hasta que se aprueba la edición, con las marcas compartidas de corrección/edición.
- Archivos JPG/PNG, video, audio y TXT usan la misma subida multipart y los límites del servidor. Cancelar la espera no afirma que el servidor descartó el envío. El mismo contenido reintenta con la misma clave para no duplicarse; un contenido nuevo obtiene otra clave.
- Nuevo GET `/api/v1/conversations/{id}` resuelve enlaces de avisos aunque el chat no esté en la página actual. Solo miembros activos/invitados reciben metadatos; un invitado aún no puede leer mensajes y una invitación rechazada deja de ser visible. Es una ampliación compatible del contrato v1.
- Verificación técnica de este incremento: React compilado, 14 casos de cliente y 107 Maven aprobados. Las comprobaciones reales de UI y sincronización se registrarán tras la ronda conjunta.
