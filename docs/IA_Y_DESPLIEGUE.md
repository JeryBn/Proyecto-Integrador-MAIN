# IA, avisos y preparación de entrega — 18/10/2026

## IA (M11/M12)
POST /api/v1/assistant, sesión de ALUMNO/DOCENTE/FAMILIAR y CSRF obligatorios.
JSON: {"clientKey":"UUID","question":"consulta de hasta 2000 caracteres"}.
Respuesta: clientKey, answer, model. Sin herramientas, acceso a chats ni búsqueda externa.
Solo se adjuntan nombres de cursos y horas autorizadas; se omiten nombres de personas, identificadores, contactos y comunicaciones.
Las preguntas son datos no confiables: no se ejecutan acciones solicitadas por el modelo. La respuesta se muestra como texto, sin HTML ejecutable.
La clave se lee de OPENAI_API_KEY o del archivo local .env.local, ignorado por Git.
Nunca configurar una clave en Android, React, gradle.properties ni argumentos públicos del cliente.

Modelo inicial configurable mediante OPENAI_MODEL: gpt-5-mini. Responses API, store=false, 1800 tokens máximos y timeout de 45 segundos; sin reintento automático al proveedor.
20 intentos por cuenta en 24 horas, incluidos fallidos. Este límite reduce gasto; no reemplaza el presupuesto del proyecto OpenAI.
Una clave clientKey no se reutiliza para otra pregunta/contexto. Una respuesta completada se devuelve sin otra llamada, durante 24 horas y solo si el contexto académico sigue vigente.
El registro local conserva hash, estado y respuesta; no guarda la pregunta. Se limpia el historial mayor de 24 horas de esa cuenta al volver a consultar. No hay todavía eliminación programada para cuentas inactivas.
Si cambia el acceso académico durante la consulta se descarta la entrega.
Cancelar en Android descarta la respuesta y permite seguir usando la app; no promete cancelar una solicitud que ya llegó al proveedor ni evita su posible coste.
No se guarda el historial de la IA en el teléfono. Cerrar sesión borra consulta, respuesta y errores de la memoria de la app.

Errores públicos sin mensajes internos del proveedor:
- AI_QUOTA_EXHAUSTED / 503: saldo o cuota agotada. No confundir con límite temporal.
- AI_RATE_LIMIT / 503: límite temporal de proveedor.
- AI_ACCESS_DENIED / 503: credencial o permisos del proyecto.
- AI_CONNECTION_ERROR / 503: red o timeout.
- AI_INCOMPLETE / 503: respuesta incompleta.
- AI_DAILY_LIMIT / 429: presupuesto local de intentos.
- AI_REQUEST_EXISTS / 409: solicitud usada, en curso o contexto modificado.
Los tests controlados sustituyen el proveedor; no se contabilizan como respuesta real de OpenAI.

Prueba real del 18/10/2026: OpenAI devolvió HTTP 429, code credit_balance_exhausted, type insufficient_quota.
No hubo respuesta académica generada. Se necesita activar saldo en https://platform.openai.com/settings/organization/billing.
Una suscripción de ChatGPT no cubre el consumo de API. Como opción de inicio para experimentos simples se puede evaluar gpt-6-luna; no se cambió el modelo automáticamente.

Fuentes consultadas:
- https://developers.openai.com/api/docs/guides/text
- https://developers.openai.com/api/docs/models/gpt-5-mini
- https://developers.openai.com/api/docs/guides/moderation

M11 sigue parcial: el asistente está conectado pero la moderación automática multimodal todavía no está implementada. La revisión humana previa de mensajes sigue vigente; no se publica automáticamente por ausencia de IA.

## Avisos (avance M10/M14)
GET /api/v1/notifications?offset=0&limit=30 (máximo 100).
POST /api/v1/notifications/{id}/read, sesión y CSRF.
Buzón persistente, orden descendente, lectura idempotente, aislamiento por destinatario.
El aviso de mensaje solo se registra en la transacción de la primera aprobación; editar no crea duplicados.
Invitaciones repetidas no duplican avisos. Una reinvitación al mismo grupo reutiliza el aviso anterior; no genera un aviso nuevo.
No se incluye el texto del mensaje en el contrato del aviso.
Se respetan preferencias, silencio y bloqueo en creación y consulta. Se reevalúa membresía y acceso al historial al consultar.
Android sincroniza al abrir o actualizar Mensajes. Todavía no hay push, avisos del sistema Android, recordatorios ni garantía de entrega en segundo plano. M10 no está terminado.

## Preparación de despliegue (M16)
Perfil production con MySQL persistente obligatorio, TLS VERIFY_IDENTITY hacia MySQL, sin demos, cookies Secure/HttpOnly/SameSite y Flyway sin baseline automático.
Variables: MAIN_DB_HOST, MAIN_DB_PORT (3306 por defecto), MAIN_DB_NAME, MAIN_DB_USER, MAIN_DB_PASSWORD.
El servidor se liga a 127.0.0.1:8088. deploy/Caddyfile propone HTTPS frente al backend en el mismo host; requiere MAIN_DOMAIN, DNS, puertos 80/443 y Caddy instalado.
Configurar el administrador mediante las variables ya documentadas del servidor y usar secretos externos al repositorio.
No aplicar Flyway sobre una base existente sin inventario, respaldo y acuerdo de propiedad con Michael.
Pendiente: elegir y aprovisionar hosting/dominio, verificar certificado, DB real, respaldo/restauración y acceso desde dos dispositivos. La configuración no demuestra un despliegue operativo.

## Firma Android (M17)
Gradle admite MAIN_SIGNING_STORE_FILE, MAIN_SIGNING_STORE_PASSWORD, MAIN_SIGNING_KEY_ALIAS y MAIN_SIGNING_KEY_PASSWORD.
La compilación release exige las cuatro variables y un archivo existente. Debug conserva su firma de desarrollo.
No se ha creado una clave de entrega ni distribuido un APK de producción. Es necesario custodiar y respaldar la clave antes de distribuir; perderla compromete actualizaciones.
