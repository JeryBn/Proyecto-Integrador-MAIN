# Guía de traspaso y comprobación web ↔ Android

Corte: 22 de octubre de 2026. Trabajo asistido por encargo de Jery Becerra Ninaquispe. React original y responsabilidad M21: Pamela Regina Meneses Mayhua. Backend/administración y pruebas compartidas: Michael Julian Fernandez Camarena y Jery según Trello. Este incremento no reasigna autores ni acredita aceptación del equipo.

## Qué se comparte

La web de usuarios y Kotlin consumen Spring Boot, API v1 y la misma MySQL. React administrativo consume Django; no usa la API administrativa histórica de Spring. Cada petición usa su propia sesión y autorización de servidor. La web añade la identidad esperada para impedir que un borrador de otra pestaña se escriba con una cuenta distinta.

| Función | Dónde probar en web | Contrato compartido |
| --- | --- | --- |
| Actividades y detalle | Inicio, búsqueda, filtros y Ver detalle | `/api/actividades/pendientes`, `/api/actividades/{id}` |
| Cursos, notas y horario | Inicio o Cursos y notas; docente registra/edita | `/api/v1/agenda`, `/grades`, `/teaching/courses/{id}/grades` |
| Privados y grupos | Chats / Nuevo chat; contactos autorizados | `/api/v1/conversations`, `/direct`, `/groups`, invitaciones y miembros |
| Texto y adjuntos | Conversación; editar, corregir o cancelar versión | `/api/v1/conversations/{id}/messages`, `/api/v1/messages/{id}` |
| Estados | Estados; texto, foto, video/audio, audiencia y visor | `/api/v1/statuses`, `/{id}/seen`, `/{id}/media`, `/{id}/delete` |
| Respuesta a estado | Responder por mensaje en el visor de un contacto | Conversación privada con el propietario; no publica una respuesta en el estado |
| Perfil y privacidad | Perfil; tema, avisos, audiencia, bloqueo/silencio y reportes | `/api/v1/preferences`, `/contacts`, preferencias/reportes por contacto |
| Avisos | Botón Avisos en cabecera, también en pantalla pequeña | `/api/v1/notifications`, `/{id}/read` |
| Asistente | IA; consulta, cancelar espera y recuperar | `/api/v1/assistant`; clave solo en servidor |

Las rutas abreviadas de la tabla conservan el prefijo del primer endpoint de su fila. El GET nuevo de metadatos `/api/v1/conversations/{id}` permite abrir avisos fuera de la primera página. Un invitado obtiene metadatos, no mensajes.

## Reglas que cada incremento debe conservar

1. Emisor ve su contenido pendiente/rechazado y motivo; otro usuario solo recibe una versión aprobada. Corregir vuelve a revisión y mantiene la marca de corrección o edición.
2. Selección de archivo no equivale a entrega: se valida, procesa y revisa. Mensajes: JPG/PNG, video/audio y TXT; estados: texto, imagen, video/audio. Límites del servidor: 12 MB y 60 segundos para multimedia; TXT 1 MB.
3. Solo alumno/docente publica estados. Familiar consulta únicamente vínculos y contenido autorizado. Bloqueos, audiencia, membresía y expiración se verifican en servidor.
4. Borradores y respuestas permanecen en memoria; recargar/cerrar la página los descarta. No se guardan credenciales ni chats en almacenamiento del navegador.
5. Cancelar una espera no demuestra que el servidor descartó un envío. Reintentar contenido idéntico conserva la clave idempotente. No reenvía automáticamente una escritura incierta.
6. Las confirmaciones se realizan dentro de MAIN, con Cancelar acción, Confirmar acción y Escape. La API vuelve a validar permisos y revisión al confirmar.

## Recorrido manual para Jery

Abrir web local en http://127.0.0.1:8088/ y administración en http://127.0.0.1:8090/. El emulador usa el puente local de ambos puertos. Usar cuentas de prueba autorizadas; no publicar contenido de estudiantes reales durante QA.

- Alumno: revisar Inicio, tareas/detalle, cursos/notas y horario. Docente: registrar y editar una nota del curso asignado; alumno actualiza y ve el nuevo valor.
- Chats: crear privado desde contacto, grupo e invitación; aceptar con otra cuenta. Administrador promueve otro, prueba retirar/salir; el último administrador debe conservar el grupo hasta designar reemplazo.
- Enviar texto/archivo: antes de revisión, comprobar ausencia en receptor y avisos. Administración revisa la versión exacta y archivo; después aparece en la otra plataforma. Rechazar, corregir y aprobar comprueba las marcas.
- Estados: crear texto/foto/video/audio; verificar revisión, audiencia privada/contactos, vistos, anterior/siguiente, pausa y respuesta privada. Eliminar únicamente el estado sintético propio preparado para esa prueba.
- Perfil: tema, silencio, bloqueo y filtro de bloqueados. Restaurar preferencias sintéticas al terminar. Familiar no obtiene el botón de publicación; usuario ajeno tampoco accede pegando el enlace de un chat.
- Dos pestañas comparten sesión de navegador: cambiar de cuenta en una debe descartar el contenido anterior en la otra. No reutilizar un borrador de la cuenta previa.
- IA: la prueba real actual devuelve falta de cuota/saldo. No considerarlo una respuesta generada. Resolver facturación/límite del proyecto antes de repetir explícitamente una consulta sintética.

## Repetir pruebas automáticas sin datos personales

- Cliente: desde `frontend`, `node build.mjs` y `node --test test/*.test.*`.
- Spring: `mvn verify`; usa base de pruebas desechable. En este equipo se usa Maven sin red y repositorio local ya descargado.
- Django: `python manage.py test administration --settings=main_admin.test_settings --noinput -v 2`; SQLite de pruebas, no MySQL compartido.
- Fixtures HTTP: `scripts/preparar_paridad_web.py seed` crea cuentas/curso/contactos sintéticos nuevos por Django. Requiere `MAIN_TEST_ADMIN_PASSWORD`, `MAIN_TEST_WEB_PASSWORD=SyntheticWebParity2026!` para el contrato instrumentado y opcional `MAIN_WEB_PARITY_FIXTURES` como carpeta local; nunca poner claves reales en argumentos ni en Git. `inspect` registra únicamente esos fixtures. `approve-message`, `reject-message`, `approve-status` rechazan IDs cuyo autor no pertenece al manifiesto.
- `WebParityBridgeTest` recibe `webStudent`, `webPeer`, `webOutsider`, `webConversation`, `webMessage`, `webAttachment`, `webCourse`, `webGroup`. Los IDs de mensaje/archivo deben salir de la UI web y la corrección aprobada debe ser revisión 2. El grupo debe conservar al compañero como administrador y haber revocado la membresía del alumno. Pasar los parámetros con `-Pandroid.testInstrumentationRunnerArguments.<nombre>=<valor>` a `connectedDebugAndroidTest`. Añadir `adminOrigin` si Django no está en 8090. Puentes ADB 8088 y 8090, una sola ejecución conectada a la vez.
- Suite real ejecutada: `SharedArchitectureUiTest`, `DetailContractTest`, `TeacherAndStatusUiTest`, `MessageAttachmentUiTest`, `WebParityBridgeTest`. `sharedActivityId` proviene de la comprobación previa Django/MySQL; nunca sustituirlo con un registro de producción.

## Límites pendientes

La paridad aquí cubre las funciones de la API y los flujos web comprobados. No certifica cámara/micrófono de navegador ni teléfono físico: la web sube archivos seleccionados; Android tiene captura nativa de foto/video. No hay notificaciones push con la aplicación cerrada: se consultan avisos dentro de MAIN y se actualiza al volver a una pestaña visible. No hay PDF/Office ni grabadora de voz nueva. Tampoco se ha realizado una prueba de carga sostenida, red móvil prolongada, todos los navegadores o aceptación académica. IA bloqueada por cuota; despliegue HTTPS, firma de entrega, revisor de apelaciones y retención siguen siendo decisiones pendientes ya registradas.

Cada nuevo módulo debe aportar contrato, pruebas positivas/negativas, captura de ambas plataformas y límites explícitos antes de avanzar su tarjeta. Evidencias actuales: `docs/informes/2026-10-22-paridad-web/`.

Comprobación tras reinicio: python scripts/verificar_paridad_web.py --fixtures <carpeta-local> --result <resultado.json>; la carpeta debe incluir fixtures.json y bridge.json de la ronda web. Requiere MAIN_TEST_WEB_PASSWORD; no crea ni aprueba contenido. La prueba negativa familiar usa multipart válido.

## Ejemplos del contrato (solo datos sintéticos)

```json
// POST /api/v1/conversations/direct
{"username":"web_peer_<id_sintetico>"}
// POST /api/v1/conversations/<id>/messages
{"clientKey":"<UUID_del_intento>","text":"WEB-PARITY mensaje de prueba"}
// PUT /api/v1/teaching/courses/<id>/grades
{"studentId":123,"assessment":"Evaluación de prueba","score":19,"maximum":20,"expectedRecordedAt":"NEW"}
```

Para editar una nota existente, reutilizar su `recordedAt` exacto; NEW solo crea. Estado: multipart con `clientKey`, `text`, `background=#087f68`, `audience=CONTACTS|PRIVATE` y `file` opcional; la respuesta inicial es pendiente, no publicada. Los ejemplos son orientativos y requieren CSRF, sesión y IDs autorizados del manifiesto; no ejecutarlos con cuentas personales.

Versión UI de este corte: `ba3606e`; Android de producción conserva el código previo, con pruebas actualizadas/ampliadas en `648e905` y `f336c8f`. La paleta usa verde MAIN `#087f68`, superficies claras/oscuras y tipografía Inter con alternativas del sistema; los componentes y puntos de adaptación están en `frontend/src/workspace.css`, sobre el React original. No se copia código de interfaz Kotlin: se comparte el contrato y las reglas.
