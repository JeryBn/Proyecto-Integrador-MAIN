# Paridad de la web de usuarios con MAIN Android

Trabajo asistido por indicación de Jery Becerra Ninaquispe. M21 conserva su asignación original a Pamela Regina Meneses Mayhua y el React existente. La administración Django ya migrada se mantiene separada; los clientes de usuarios consumen Spring y la misma base MySQL.

## Matriz de implementación y verificación

| Módulo | Android y API existentes | Web en este incremento |
| --- | --- | --- |
| Acceso, actividades, agenda y notas | Implementados; evidencia histórica y 3 Android/MySQL del corte anterior | Conservados; inicio agrupa tareas y cursos/notas; regresión visual final pendiente |
| Perfil, ajustes y contactos | Preferencias y contactos en API v1 | Implementados: tema, avisos, audiencia, bloqueo/silencio y reportes; pruebas de cliente correctas; recorrido real final pendiente |
| Avisos | Lista y lectura paginadas por permisos | Implementados con actualización visible y paginación; recorrido real final pendiente |
| Privados y grupos | Conversaciones, invitaciones, roles, revisión y mensajes v1 | Pendiente de implementación en este corte |
| Adjuntos | Imagen, video, audio y TXT; revisión previa | Pendiente de interfaz web |
| Estados | Texto/multimedia; audiencia y caducidad; familiar no publica | Pendiente de interfaz web |
| Asistente | Servicio existente y clave autorizada; cuota agotada en corte previo | Pendiente de interfaz y comprobación real del proveedor |

## Primer incremento de perfil y sesión

- Cliente común usa credenciales del mismo origen, respuestas privadas sin caché, CSRF renovado en cada escritura y errores estructurados. No reintenta automáticamente una escritura de resultado incierto.
- `X-MAIN-Expected-User` protege la cuenta mostrada. Si otra pestaña cambia la sesión, Spring rechaza lectura/escritura con 409 `ACCOUNT_CHANGED` antes del controlador y el cliente descarta el estado de la página. Clientes Android anteriores que no envían esta cabecera siguen compatibles.
- El filtro se ejecuta antes del cierre de sesión; las preferencias y notas web comparten el cliente y no pueden escribir un borrador usando accidentalmente otra identidad.
- Tema y preferencias persisten en servidor, sin guardar información académica ni credenciales en almacenamiento del navegador. Las consultas periódicas se detienen cuando la pestaña está oculta y se actualizan al volver.
- Verificación técnica: React compilado, 11 pruebas de cliente (4 de acceso existentes y 7 nuevas) y 105 Maven aprobadas. No se declara paridad global ni aceptación con estos resultados parciales.
