# Circuito de apelaciones de MAIN — 27 octubre 2026

Implementación técnica comprobada con datos sintéticos en web, Android y administración Django. No depende de cuota OpenAI ni de Firebase. No acredita revisión de contenido real por un proveedor externo ni despliegue público.

## Recorrido disponible

1. El propietario de un mensaje o estado rechazado pulsa **Apelar rechazo**, explica el contexto y envía la solicitud. Spring comprueba propietario, habilitación, estado, versión, huella y que el contenido no haya sido eliminado. Otra cuenta no puede apelar ese contenido ni consultar el historial del solicitante.
2. La apelación aparece en **Mis apelaciones**, dentro de Perfil/Ajustes, y en la cola administrativa. Cada versión admite una apelación; los reintentos devuelven la misma solicitud y no cambian su motivo ni reabren una resolución. Una corrección crea una versión distinta.
3. Una apelación OPEN impide que la retención elimine el contenido, sus archivos y su auditoría. La protección manual no puede anularla. El bloqueo comparte la conversación/estado con revisión, edición y limpieza.
4. Otro ADMIN, distinto de quien rechazó el contenido y de su propietario, revisa la versión exacta y sus archivos. Debe explicar la resolución. Puede **mantener el rechazo**, **devolver a revisión humana** o **cerrar por cambio/caducidad** cuando corresponda.
5. Devolver a revisión cambia esa versión a PENDING, mantiene privado el contenido y reinicia su plazo de revisión. No envía mensajes, no publica estados ni notifica a destinatarios. Una aprobación humana posterior verifica nuevamente permisos, versión y archivo antes de entregarlo.
6. El solicitante consulta la explicación y fecha de resolución en web o Android. El historial resuelto se minimiza a los noventa días, salvo protección abierta sobre el asunto. Resolver una apelación concede un plazo nuevo de retención al contenido conservado.

Un estado caducado no recibe veinticuatro horas nuevas por apelar ni puede devolverse a publicación. Se conserva su evidencia mientras se revisa y puede cerrarse con una explicación. Una edición o cancelación tampoco permite que el revisor republique una versión antigua. La retirada explícita por el propietario sigue siendo una operación separada de la limpieza automática.

## Contratos y permisos

- Flyway V16 crea moderation_appeal sobre la misma MySQL compartida. Spring crea solicitudes y devuelve solo el historial de la identidad autenticada: GET/POST `/api/v1/appeals`, paginación 30 por defecto. POST recibe kind, subjectId, revision (cero para estados), contentHash y reason (1–1000 caracteres).
- Django gestiona GET `/api/admin/v1/appeals`, detalle `/<id>`, archivo `/<id>/media` y POST `/<id>/resolve`. La resolución requiere outcome UPHELD/RETURNED/SUPERSEDED, reason y contentHash. Sesión ADMIN y CSRF en escrituras. Los archivos tienen no-store y verificación de integridad; no se devuelven desde esta ruta después del cierre.
- La resolución y su auditoría son atómicas. La auditoría guarda actor, acción e identificador, sin copiar motivos ni contenidos. La cuenta web SQL solo puede leer apelaciones y actualizar las cuatro columnas de resolución; no puede insertar solicitudes ni borrar su historial. La cuenta separada de retención puede leer y eliminar historial vencido, sin modificar identidades.
- Se actualizó database/retention-grants.sql: aplicar después de V16. Las consultas de retención excluyen apelaciones OPEN antes de limitar los lotes. No activar un trabajador antiguo V15 mientras se usa el circuito V16.

## Evidencia comprobada

| Verificación | Resultado |
|---|---:|
| Django, batería completa | 69 aprobadas |
| Spring, batería completa | 147 distintas, sin errores, fallos ni omitidas |
| Casos nuevos Spring de solicitudes de apelación | 6, incluidos en 147 |
| Web, pruebas existentes de contrato/utilidades y compilación React | 37 aprobadas y compilación correcta |
| Android, pruebas unitarias y compilación debug | 4 aprobadas y compilación correcta |
| Android, prueba visual de circuito sobre servidor real/MySQL | 1 aprobada en emulador |
| HTTP real de mensaje y estado, privacidad y resolución | 25 comprobaciones aprobadas |

El ensayo web utilizó la interfaz para solicitar la apelación y otro ADMIN sintético para devolverla a revisión. Las comprobaciones HTTP confirmaron privacidad, rechazo al revisor original, CSRF, protección contra limpieza y liberación manual indebida. Solo después de otra aprobación humana se entregó el mensaje, con un único aviso al destinatario. Se repitió el recorrido de un estado por las APIs reales, sin visibilidad para el contacto hasta esa nueva aprobación.

La prueba visual Android creó su propio estudiante/grupo sintéticos, envió y recibió un rechazo, presentó la apelación desde la app, leyó su resolución independiente y verificó la nueva aprobación. Se conserva evidencia en docs/informes/2026-10-27-apelaciones. La prueba fue en emulador; no sustituye aceptación en teléfono físico.

scripts/verificar_apelaciones.py prepara fixtures de loopback y verifica fases open/resolved/statuses; las fases de envío y resolución web se ejercitaron con el navegador. AppealUiTest requiere el argumento appealReviewer con un ADMIN sintético independiente provisionado solo para el ensayo. No ejecutar estos fixtures con datos reales ni copiar sus contraseñas de demostración a cuentas operativas.

## Operación y pendientes reales

El revisor temporal del ensayo fue deshabilitado al terminar. No se crearon cuentas ADMIN operativas para Jery/Michael ni se concedieron permisos administrativos a compañeros reales. Para uso institucional faltan esas identidades verificadas: la designación de personas no equivale a provisionar sus cuentas. La implementación y los ensayos del circuito sí están terminados; su puesta en operación real no se atribuye a los fixtures.

El proceso horario de retención se reinicia con V16 y permisos actualizados. Sigue siendo local y debe reiniciarse al cerrar Windows; todavía falta operación supervisada en el destino HTTPS. Permanecen los bloqueos previos de cuota IA, credencial FCM, despliegue y aceptación en teléfono físico. M19/M20 no se cierran globalmente por aprobar este incremento: aún falta evaluar moderación automática real y vincular los revisores operativos.

Trello actualizado: descripciones y comentarios de M19/M20 distinguen apelaciones terminadas de operación/evaluación pendientes. En M20 se registraron ocho criterios de este incremento: seis verificados marcados y dos pendientes sin marcar. La tarjeta global sigue abierta; la evidencia de la checklist se conserva en trello-m20-progress.png. No se modificaron fechas históricas ni se atribuyó aceptación al docente o a compañeros.
