# Push Android y moderación automática

Incremento del 25/10/2026 posterior al informe de regularización de sprints 1 y 2. Implementado por encargo de Jery; no modifica autoría ni acredita entrega al 20/10. La configuración y verificación siguientes reemplazan las referencias anteriores a ausencia total de push o evaluación automática.

## Estado comprobado

| Parte | Resultado | Pendiente |
|---|---|---|
| Firebase | Se encontró sesión CLI válida y solo el proyecto ETERNA; se creó MAIN separado, ID `main-integrador-jery-2026`, sin vincular facturación. Android `pe.tecsup.main.mobile` registrado. | Credencial de servicio para el servidor y prueba de envío. No se usó ETERNA. |
| Configuración Android | Configuración oficial descargada a `mobile/app/google-services.json`, ignorada por Git. Firebase BoM 35.1.0, Messaging y plugin 4.5.0 compilan. | Conservar copia privada de configuración y configurarla en cada entorno de compilación. |
| Registro Firebase real | Emulador Android 17/API 37 obtuvo token real; dos pruebas instrumentadas aprobadas, ninguna omitida. El token no aparece en evidencia ni Git. | Recibir un push enviado por MAIN con app en segundo plano/cerrada y en teléfono físico. Obtener token no acredita recepción. |
| Backend push | API autenticada/CSRF, tokens cifrados, cola con deduplicación/lease/reintentos y comprobación de sesión/permisos. | Configurar credencial FCM y habilitar envío tras probar. |
| Moderación OpenAI real | Texto educativo sintético recibió CLEAR; imagen generada sintética recibió HUMAN_REQUIRED por política de cobertura parcial. Sin cambios a BD. | Evaluación con casos representativos, políticas aprobadas y responsable. No acredita precisión global del clasificador. |
| Moderación automática | Comando Django con modo shadow, presupuesto de tres intentos por versión/política, caché y cola sin bloqueo por casos ya evaluados. Publicación de texto claro solo con --apply y política aprobada. | Activación institucional; imágenes requieren revisión humana y audio/video no se envían al proveedor en esta implementación. |

La revisión humana continúa disponible. No se eliminan contenidos automáticamente ni se envían contenidos reales al proveedor por defecto.

## Push implementado

1. En Perfil, el usuario pulsa Activar avisos del teléfono. Android 13 o superior solicita permiso; denegarlo conserva el buzón interno.
2. Se obtiene el token de Firebase y se registra con sesión/CSRF en `POST /api/v1/push/devices`. La identidad procede de la sesión, nunca del cuerpo. `GET /api/v1/push/configuration` informa disponibilidad; DELETE del dispositivo solo opera sobre el dueño.
3. Flyway V10 crea dispositivos y entregas. El token usa AES-256-GCM con AAD por dispositivo; BD guarda hash de sesión, no cookie ni contraseña. Máximo diez dispositivos por cuenta.
4. Un proceso del servidor busca avisos aprobados posteriores al registro, vigentes hasta 24 horas, no leídos y aún autorizados. Recomprueba cuenta, preferencias, bloqueo/silencio y membresía antes de enviar. No hace push de pendientes/rechazados.
5. FCM recibe únicamente token, binding aleatorio, identificador de entrega derivado y tipo MAIN_UPDATE. No recibe texto, nombre, curso ni ID de conversación. OAuth 2 con JWT de cuenta de servicio y endpoints HTTPS fijos, sin redirecciones.
6. Cada entrega tiene clave única dispositivo/aviso, lease de dos minutos y máximo cinco intentos separados por dos minutos. Un token UNREGISTERED se elimina. Aceptación HTTP de FCM no significa recepción en el dispositivo. Un timeout puede generar redelivery; la notificación local se colapsa con ID estable.
7. Android solo muestra un aviso genérico si el binding corresponde al ingreso actual. Cerrar sesión o desactivar localmente elimina binding y notificaciones; una entrega tardía de otra sesión se descarta. Al abrir MAIN se consulta contenido autorizado con nueva sesión cuando corresponda.

Límites: no hay cookies/contraseñas persistidas en Android. El registro requiere sesión de servidor activa: caduca por su inactividad configurada, normalmente 30 minutos, o como máximo 24 horas desde registro. Este incremento no introduce inicio de sesión persistente ni push web. Un aviso ya aceptado por FCM puede estar en tránsito hasta cinco minutos tras un cambio de preferencias; no lleva contenido privado. Forzar detención desde ajustes del sistema y restricciones de Android pueden impedir recepción. Al cambiar entre cuentas tras un cierre remoto fallido puede ser necesario esperar a que caduque el registro anterior.

## Configuración de push que falta

El proyecto y la configuración Android ya existen; no hay que crear otro proyecto ni otra app.

1. En Firebase Console seleccionar **MAIN Integrador** → Configuración del proyecto → Cuentas de servicio. Obtener una credencial de servidor autorizada para Firebase Cloud Messaging HTTP v1. La cuenta necesita `cloudmessaging.messages.create`, mediante un rol apropiado como Firebase Cloud Messaging API Admin; no asignar Owner a la app Android.
2. Guardar el archivo JSON de cuenta de servicio **fuera del repositorio**, en una carpeta privada accesible al usuario que ejecuta Spring. Indicar solo su ruta local. No pegar el contenido en chat/Trello ni incluirlo en APK/web.
3. En `.env.local` del backend configurar `MAIN_FCM_CREDENTIALS_FILE` con esa ruta y, tras la revisión, `MAIN_PUSH_ENABLED=true`. La clave local `MAIN_PUSH_ENCRYPTION_KEY` ya está generada y guardada allí sin mostrarla; respaldarla de forma privada. La rotación necesita re-registrar dispositivos.
4. Confirmar que Firebase Cloud Messaging API está habilitada para este proyecto, que el servidor sale por HTTPS a OAuth/FCM y que Android tiene acceso a Google Play Services y al servidor MAIN. Elegir entorno HTTPS para prueba entre dispositivos.
5. Compilar con el JSON Android local, instalar, ingresar, activar avisos, aprobar un mensaje sintético desde otra cuenta y comprobar recepción con app abierta, en segundo plano y cerrada; después probar logout, bloqueo, silencio, permiso denegado, token revocado y red interrumpida. No declarar M10 terminada antes de guardar resultados.

El proyecto se creó sin activar facturación. No se realizó envío FCM real ni se generó una clave de cuenta de servicio. Esa credencial es el acceso concreto que falta; la clave de API pública de Android no la sustituye.

## Moderación automática implementada

Se ejecuta en Django, respetando React → Django para administración y Spring para usuarios. Flyway V11 es dueño de `automatic_assessment`; el trabajador necesita SELECT/INSERT/UPDATE en esa tabla y los permisos administrativos de revisión ya documentados. No genera DDL desde Django.

`manage.py moderate_pending --limit 10` evalúa en shadow. `--apply` permite publicar únicamente texto/TXT que el proveedor considere claro. Antes de leer la cola o llamar al proveedor se exige política aprobada, versión, revisor ADMIN activo y días de conservación válidos. La identidad de auditoría automática es MAIN_AUTOMATION, distinta de una revisión humana.

Variables de entorno del proceso Django:

- `OPENAI_API_KEY`: reutilizar de forma privada la clave existente; no se creó otra. El comando no carga automáticamente `.env.local` de Spring.
- `MAIN_MODERATION_POLICY_APPROVED=true`: activar solo después de decisión explícita del responsable.
- `MAIN_MODERATION_POLICY_VERSION`: versión aprobada, entre 1 y 80 caracteres.
- `MAIN_MODERATION_REVIEWER`: usuario ADMIN existente y activo que puede revisar casos y apelaciones. Registrar además suplente y alcance de acceso en la política.
- `MAIN_MODERATION_RETENTION_DAYS`: entero entre 1 y 3650, decidido por el responsable. Esta validación **no implementa borrado** ni equivale a una política completa de retención de rechazados, pendientes, auditoría o evaluaciones.

La clave, el contexto de cursos, las identidades y el historial nunca forman parte de la carga enviada a moderación: solo texto y/o imagen de la versión evaluada. Texto/TXT de más de 20000 caracteres y adjuntos no compatibles van a revisión humana, sin truncarlos como si se hubieran analizado completos. Imagen hasta 5 MB, JPG/PNG verificado por la capa de medios existente; el trabajador valida además hash e integridad de los bytes. No se envían URLs privadas ni nombres de archivos al proveedor.

La respuesta exige esquema completo y puntuaciones válidas. Flagged o cualquier categoría marcada mantiene PENDING para revisión humana, sin rechazo irreversible. Imágenes siempre requieren revisión humana porque la cobertura de categorías visuales no incluye todos los riesgos de texto/OCR/audio. Video/audio se derivan directamente al revisor: no se presentan muestreo de frames ni transcripción parcial como revisión completa.

Si hay respuesta inválida, timeout, falta de cuota/acceso o proveedor caído, el contenido queda privado. Se limita a tres intentos por contenido/política, con espera de cinco minutos; se detiene la tanda ante fallo del proveedor. Caché y lease evitan llamadas simultáneas o repetidas; estados ya evaluados no bloquean la cola posterior. Cambiar versión/hash exige otra evaluación. La publicación reutiliza bloqueos, permisos, hash, revisión, transacción, auditoría y generación de avisos del flujo humano; una revisión obsoleta no se publica.

Prueba real sin datos reales y sin cambios de BD: `manage.py moderate_pending --synthetic-probe` y, para imagen generada, añadir `--synthetic-image`. Ejecutar con entorno de prueba o del servidor configurado. No activar el trabajador periódico sobre la cola real hasta aprobar la política, custodia y operación. No se instaló un planificador recurrente ni una purga automática.

## Resultados y defectos encontrados

- Spring: **127 ejecuciones aprobadas**, 118 casos distintos; nueve ejecuciones adicionales repiten el caso de retry push diez veces en total. Cero fallos/errores/omitidas en la última ronda. Una ronda anterior detectó carrera de precisión temporal en la cola; corregida y repetida.
- Django: **43 pruebas aprobadas**, incluidas once nuevas de moderación automática sobre las 32 anteriores.
- Android: **4 pruebas unitarias** y compilación/lint con Firebase configurado. Lint: cero errores, once advertencias y cuatro sugerencias en ese corte. SDK marca métodos de Messaging como deprecados, sin impedir compilación; una futura migración no cambia el resultado actual.
- Android instrumentado: **2/2 aprobadas**, cero omitidas, con `realPushProbe=true`: token Firebase real y descarte local tras logout. No es prueba de entrega FCM de servidor a teléfono.
- OpenAI: dos consultas reales sintéticas, texto e imagen, con salida estructurada válida. La moderación funcionó; eso no prueba que el asistente académico tenga cuota disponible ni una exactitud del 100 %.
- Empaquetado Spring: un intento chocó con archivo JAR ocupado; reempaquetado posterior finalizó BUILD SUCCESS. No se omitió ese fallo inicial.

## Referencias oficiales

- [Moderación OpenAI y modalidades](https://developers.openai.com/api/docs/guides/moderation)
- [FCM HTTP v1](https://firebase.google.com/docs/cloud-messaging/send/v1-api)
- [Recibir mensajes Android](https://firebase.google.com/docs/cloud-messaging/android/receive-messages)
- [Configuración Android Firebase](https://firebase.google.com/docs/android/setup)

Commits de implementación: 069b6e1, 6e7a1e0, 95787a8; correcciones/pruebas c5cc55e, a84f3ca, 870201d y 5178d39. Evidencia de pruebas y metadatos en docs/informes/2026-10-25-push-moderacion/. Ningún archivo de evidencia contiene tokens ni credenciales.
