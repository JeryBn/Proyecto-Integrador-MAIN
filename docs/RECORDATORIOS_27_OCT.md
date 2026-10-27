# Recordatorios académicos configurables

Incremento posterior al informe Word/PDF de revisión del 27 de octubre. Implementa el pendiente de recordatorios de M10 en servidor, web y Android; no acredita recepción FCM real, despliegue HTTPS ni aceptación global.

## Comportamiento

En Perfil, los alumnos pueden activar recordatorios de sus tareas pendientes y elegir 15 minutos, una hora o un día de anticipación. Están desactivados por defecto y se guardan por usuario. Docentes, familiares y administradores no pueden reclamar estas preferencias de alumno mediante API. También deben estar habilitados los avisos generales.

Un proceso interno revisa cada minuto hasta 500 tareas elegibles. Solo usa alumno activo, matrícula vigente y actividad futura sin entrega. Crea un aviso por usuario, actividad, fecha límite y anticipación; los reintentos y varias instancias no duplican ese evento. Si se cambia la fecha se invalida el aviso anterior y puede generarse uno para la nueva fecha. Cambiar la anticipación puede generar un nuevo evento para la nueva configuración. No envía recordatorios retroactivos de actividades ya vencidas.

El buzón combina mensajes, invitaciones y recordatorios, conserva paginación y permite marcar leído. Los clientes distinguen «Actividad próxima a vencer» y abren el detalle autorizado de la actividad, sin intentar abrir un chat nulo. El servidor vuelve a comprobar matrícula, entrega, preferencias y fecha al consultar el buzón y al intentar push. Revocar matrícula, entregar, desactivar o cambiar fecha oculta el aviso obsoleto. Un envío FCM ya aceptado puede seguir en tránsito según el límite anterior; el payload sigue siendo genérico.

La ruta existente de push incluye los recordatorios sin texto del curso, título o descripción en FCM. Sigue requiriendo credencial de servicio, permiso y sesión activa. Este incremento no pretende sustituir esa configuración ni afirmar recepción con el teléfono cerrado.

## Contrato y migraciones

- GET y PUT `/api/v1/reminders/preferences`: `{enabled: boolean, minutesBefore: 15|60|1440}`. Identidad de sesión, protección CSRF en PUT, validación de rol en servidor.
- `/api/v1/notifications` añade `activityId` nullable. Para recordatorio: `kind=REMINDER`, `conversationId=null`, `activityId` positivo. Los avisos de chat conservan su contrato anterior.
- Flyway V12 permite conversación nula en un aviso académico mediante migración Java específica para MySQL/H2. V13 añade campos de destino/fecha/anticipación e índice, más `reminder_preference`.
- No se guardan títulos ni descripciones en la cola. No hay purga ni nueva retención decidida en este incremento.
- Intervalo operativo `main.reminders.interval-ms`, por defecto 60000. El servidor debe permanecer disponible; si está apagado y la fecha pasa, no se promete entrega posterior.

## Verificación del corte

| Prueba | Resultado |
|---|---|
| Spring completo | 141 ejecuciones aprobadas, cero fallos/errores/omitidas; 132 casos distintos y nueve repeticiones extra de retry. Incluye seis casos nuevos de recordatorios y dos de integración con gateway push controlado. |
| MySQL real y Django/Spring por HTTP | V12/V13 aplicadas; 18 comprobaciones aprobadas, incluido proceso programado real, destino de actividad, CSRF, aislamiento, lectura y revocación. Sin envío FCM. |
| Web | 37 pruebas aprobadas y compilación React. Se verificó desde navegador oculto cambio de anticipación y persistencia tras recarga; se restauró la preferencia sintética anterior. |
| Android | Cuatro unitarias y compilación/lint aprobados. Dos instrumentadas de contrato y una de Perfil aprobadas en emulador Android 17, API 37, contra servidor real. Ninguna omitida. No es teléfono físico. |
| Medios Django | 53 pruebas aprobadas y extracción FFmpeg real sintética. Proveedor real de transcripción bloqueado por HTTP 429; detalle en MODERACION_MULTIMEDIA_27_OCT.md. |

Una primera migración SQL no era compatible con H2 y se sustituyó antes de publicar por V12 Java y V13 SQL; también se corrigió el tipo del parámetro temporal de la consulta portable. Una primera ejecución del verificador HTTP usó ruta administrativa incorrecta, corregida antes de las 18 comprobaciones exitosas. Esos intentos fallidos no se contabilizan como aprobados. Las entidades de prueba están identificadas como sintéticas; no se alteraron matrículas ni actividades de personas reales.

## Reproducción y continuación

Ejecutar `mvn test` con Java 21. Web: `node --test test/*.test.*` y `node build.mjs` desde frontend. Android: `testDebugUnitTest assembleDebug lintDebug` y seleccionar ReminderContractTest/ReminderUiTest en connectedDebugAndroidTest con puentes ADB a 8088/8090 y emulador arrancado. El verificador `scripts/verificar_recordatorios_local.py` necesita los servidores reales y la contraseña del ADMIN sintético en variable privada, nunca por argumento o en Trello. Espera como máximo 75 segundos al programador y conserva la evidencia de la ronda.

Para terminar M10 siguen faltando credencial FCM, recepción abierta/segundo plano/cerrada en teléfono, revocación/red real, destino final HTTPS y aceptación. Para terminar M19/M09/M11/M20 faltan política aprobada, revisor/suplente/retención, evaluación de cobertura y transcripción real disponible. Los informes anteriores son cortes históricos; este documento y MODERACION_MULTIMEDIA_27_OCT.md describen el avance posterior.
