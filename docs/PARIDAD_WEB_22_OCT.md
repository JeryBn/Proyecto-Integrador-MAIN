# Paridad de la web de usuarios con MAIN Android

Trabajo asistido por indicación de Jery Becerra Ninaquispe. M21 conserva su asignación original a Pamela Regina Meneses Mayhua y el React existente. La administración Django ya migrada se mantiene separada; los clientes de usuarios consumen Spring y la misma base MySQL.

## Matriz de implementación y verificación

| Módulo | Android y API existentes | Web en este incremento |
| --- | --- | --- |
| Acceso, actividades, agenda y notas | Implementados; evidencia histórica y 3 Android/MySQL del corte anterior | Conservados; inicio agrupa tareas y cursos/notas; recorrido web real aprobado: tareas, búsqueda/detalle, nota 18→19 y horario; Android lee 19 tras guardar en web |
| Perfil, ajustes y contactos | Preferencias y contactos en API v1 | Implementados: tema, avisos, audiencia, bloqueo/silencio y reportes; tema, bloqueo con revocación de estados, silencio/restauración y reporte sintético probados en navegador |
| Avisos | Lista y lectura paginadas por permisos | Implementados con actualización visible y paginación; lista, lectura y enlace a conversación probados; avisos accesibles en 390 px |
| Privados y grupos | Conversaciones, invitaciones, roles, revisión y mensajes v1 | Implementados: búsqueda, privados, grupos, invitaciones, miembros, roles, salida, edición/corrección/cancelación; grupo creado, invitación aceptada, promoción y salida probadas; Android respeta membresía revocada |
| Adjuntos | Imagen, video, audio y TXT; revisión previa | Subida, progreso, cancelación/reintento idempotente y visor autorizado implementados. TXT web leído en Android; foto Android cargada en web; video/audio de estados web descargados en Android; los cuatro tipos de mensaje pasan la suite Android/API |
| Estados | Texto/multimedia; audiencia y caducidad; familiar no publica | Creación, audiencia, visor/secuencia, vistos, eliminación y borrador implementados; texto/foto/video/audio subidos desde web; visor y privacidad, vistos, eliminación y respuesta privada probados; cruce Android aprobado |
| Asistente | Servicio existente y clave autorizada; cuota agotada en corte previo | Interfaz conectada, consulta/recuperación/cancelación y error real del servicio implementados; consulta real devolvió cuota/saldo agotado; no hay respuesta generada verificada |

## Resultado final de este incremento

- 39 comprobaciones de navegador registradas en `browser-checks.json`; 33 HTTP tras reinicio en `after-restart.json`.
- Regresión: 107 Spring, 32 Django, 19 cliente y 9 casos distintos Android/MySQL aprobados. El puente ampliado de grupos/medios se repitió aparte y también pasó; no se suma como décimo caso distinto.
- En ambos sentidos: mensaje y estado creados en Android llegan a web tras moderación; mensaje corregido y TXT web se leen en Android. Video/audio web se descargan en Android; foto Android aparece con 8 px en web. Privacidad, usuario ajeno, familiar y grupo revocado conservan autorización del servidor.
- Datos conservados tras reiniciar el JAR: notas, texto/medios, roles y preferencias. Los recursos HTTP coinciden por SHA-256 con la compilación empaquetada; no se depende del directorio externo de desarrollo.
- Confirmaciones internas con Cancelar/Confirmar/Escape verificadas; un diálogo nativo bloqueó la pestaña de pruebas anterior. El flujo ahora evita ese bloqueo. Se corrigieron también avisos móviles, refresco académico e invitaciones duplicadas.
- IA real bloqueada por falta de cuota/saldo del proyecto de OpenAI. Se conserva la clave autorizada; no se crea otra ni se informa éxito falso.
- Límites: no se certifican cámara/micrófono de navegador (subida de archivo), teléfono físico, push con app cerrada, carga sostenida, red móvil prolongada o aceptación humana. Son pendientes separados; las tarjetas globales pasan a revisión, no a finalización total.

[Guía de traspaso y repetición](GUIA_PARIDAD_WEB_ANDROID.md). [Resultados y capturas](informes/2026-10-22-paridad-web/LEEME.md). Los apartados siguientes describen la evidencia incremental en el momento de cada commit, anterior a la ronda final.

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

## Incremento de estados

- Texto con fondo/contraste y multimedia de imagen, video o audio, con el mismo límite de 700 caracteres, 12 MB y 60 segundos aplicado por servidor. Borrador y clave idempotente conservados mientras la página permanece abierta.
- Audiencia por estado y preferencia global, indicador pendiente/rechazado, razón privada y conteo de vistas del propietario. Solo estudiantes/docentes tienen creación; el servidor sigue rechazando publicación familiar.
- Visor por contacto en orden temporal, anterior/siguiente, avance automático de texto/imagen con pausa, reproducción multimedia manual y detención al ocultar la pestaña. No se reproduce audio automáticamente durante el trabajo en segundo plano.
- Listados periódicos respetan respuesta autorizada del servidor, audiencia/bloqueos y expiración; una actualización que desaparece deja de mostrarse. Eliminación propia requiere confirmación.
- Verificación técnica: React compilado y 17 pruebas cliente aprobadas. No se declara aún probado el recorrido visual ni su sincronización Android en este corte.

## Incremento del asistente

- La web usa el servicio `/api/v1/assistant` existente, su contexto académico autorizado y la clave previamente creada/autorizada por Jery. No configura claves en el navegador ni crea un proveedor paralelo.
- Pregunta de hasta 2000 caracteres, explicación de datos enviados a OpenAI y límite de 20 solicitudes por cuenta cada 24 horas, iguales a Android. Pregunta, respuesta y borrador permanecen únicamente en memoria de la página.
- Cancelar la espera o perder conexión conserva la clave para recuperar una respuesta ya recibida por servidor, sin duplicar el consumo. Un error confirmado de cuota/acceso requiere una nueva solicitud explícita después de resolver el servicio; no se inventa ni se sustituye una respuesta del proveedor.
- Verificación técnica: React compilado y 19 pruebas cliente aprobadas. La respuesta real del proveedor todavía debe comprobarse; el error de cuota del corte previo no se presenta como una consulta exitosa.
