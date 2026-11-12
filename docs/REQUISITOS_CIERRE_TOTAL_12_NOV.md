# Requisitos para el cierre total de MAIN — 12 de noviembre de 2026

Este documento separa lo técnicamente terminado de las acciones externas que no pueden sustituirse con código ni marcarse como aceptadas en nombre del equipo. No contiene secretos.

## Estado técnico disponible

- Rama `feat/resiliencia-profesional` publicada, sin cambios locales pendientes.
- Spring: 157 pruebas aprobadas; Django: 83; web: 42; Android: 9 unitarias, `lintDebug` y `assembleDebug` aprobados.
- Arquitectura MySQL/Spring/Django: 16 comprobaciones HTTP reales después de reiniciar.
- Web de usuarios: 18 vistas normales y al 200 % emulado sin infracciones Axe, errores de consola ni desbordamiento global.
- Administración: seis estados normales, sin conexión y al 200 % emulado sin infracciones Axe ni desbordamiento; el acceso sin red conserva el usuario, limpia la contraseña y oculta el error técnico.
- El rediseño administrativo de Pamela está integrado en la rama de resiliencia conservando su autoría, seguido de correcciones separadas de accesibilidad.

## Acciones que debe habilitar Jery

| Bloqueo | Qué se necesita | Evidencia de cierre |
|---|---|---|
| Trello privado | Cuando Jery termine de usar la pantalla, autorizar una sesión breve con el navegador ya autenticado, o conectar un acceso de Trello a las herramientas. No compartir contraseña ni token por chat. | Estado, checklist, responsables, comentarios y enlaces a commits actualizados en las tarjetas existentes. |
| Escritura en GitHub | Permitir el navegador autenticado o conceder escritura a la integración. La API disponible devuelve `403 Resource not accessible by integration`. | Revisar e integrar PR #3 y #4, abrir el PR de `feat/resiliencia-profesional`, ejecutar comprobaciones y dejar `main` actualizado. |
| Teléfono físico | Conectar un Android 8 o superior por USB, desbloquearlo y aceptar depuración e instalación. No hay dispositivos ADB conectados en esta revisión. | Acceso, listado, detalle, permisos, medios, segundo plano, cierre de app, desconexión y recuperación documentados con fecha y dispositivo. |
| Firma de entrega | Copiar el keystore original y confirmar su alias, o aprobar expresamente la creación de una identidad nueva si nunca se distribuyó otra. Configurar contraseñas solo mediante el mecanismo privado local. La carpeta `%LOCALAPPDATA%\MAIN-private\android` no existe en esta PC. | APK release no depurable, firma y huella verificadas, respaldo privado recuperable e instalación en el teléfono sin borrar datos para ocultar incompatibilidades. |
| Android Firebase | Colocar `mobile/app/google-services.json` fuera de Git. El archivo no está presente en ninguna de las dos copias revisadas. | Compilación release con el proyecto Firebase correcto y token obtenido en el teléfono de prueba sin exponerlo. |
| FCM del servidor | Crear o proporcionar la ruta local de un JSON de cuenta de servicio autorizado para FCM HTTP v1, fuera del repositorio; habilitar la API correspondiente. | Push genérico recibido con la app abierta, en segundo plano y cerrada; además, logout, silencio, bloqueo, permiso denegado, token revocado y red interrumpida. |
| HTTPS definitivo | Elegir proveedor, dominio, presupuesto y custodio; proporcionar acceso seguro. No existe servidor público ni dominio configurado. | Certificado público válido, DNS, MySQL con TLS, respaldos/restauración, colección Postman de 55 comprobaciones y recorrido Android/web desde dos dispositivos. |
| Cuota de IA | Habilitar saldo/cuota en el proyecto ya existente. No enviar la clave por chat ni crear otra para evadir límites. | Una respuesta académica sintética correcta en web y Android; transcripción multimedia real disponible y evaluación controlada sin datos personales. |
| Moderación operativa | Vincular cuentas reales y verificadas para Jery como responsable y Michael o el suplente aceptado; aprobar el tratamiento externo y confirmar la política inicial 7/30/90. | Revisión, apelación, protección y retención comprobadas con cuentas operativas; aceptación humana de falsos positivos/negativos y cobertura multimedia. |
| Aceptación del proyecto | Pamela, Michael, Jery y el docente confirman el alcance que les corresponde. La automatización no puede firmar en su nombre. | Comentario o acta por historia con fecha, responsable, resultado, limitaciones aceptadas y decisión final. |
| Accesibilidad manual | Una persona ejecuta zoom real al 200 %, lector de pantalla, teclado completo y revisión visual en navegador/teléfono. | Evidencia manual asociada a M04/M21/M24; las auditorías Axe ya disponibles sirven de apoyo, no de sustitución. |

## Tarjetas que dependen de estas acciones

- M01 y S3-01: acuerdo de alcance, estimaciones y definición de terminado.
- M04, M21 y M24: aceptación visual, lector de pantalla, zoom real y paridad en dispositivo.
- M10: credencial y recepción FCM real, además de pruebas de revocación.
- M11, M12, M19, M20 y S3-02/S3-03: cuota, transcripción, cuentas operativas, evaluación y aceptación de política.
- M15: carga y seguridad sobre el destino final, red y dispositivos reales.
- M16, S3-04 y S3-07: servidor, dominio, TLS, operación y Android contra HTTPS final.
- M17, S3-05 y S3-06: firma recuperable, APK release y teléfono físico.
- M18 y S3-08: PR integrados, informe final, demostración y aceptación del equipo/docente.

## Orden mínimo para terminar

1. Habilitar acceso de edición a Trello y GitHub para regularizar el trabajo ya comprobado.
2. Elegir HTTPS y recuperar o crear conscientemente la firma de entrega.
3. Proporcionar las rutas privadas de Firebase Android y FCM; habilitar cuota de IA.
4. Conectar el teléfono y ejecutar la matriz completa contra HTTPS.
5. Corregir cualquier defecto hallado, repetir regresiones y generar el APK release final.
6. Recoger la aceptación real de Pamela, Michael, Jery y el docente.
7. Solo entonces completar las listas y mover las tarjetas globales a `FINALIZADO`.

Marcar todo como completo antes de estas evidencias produciría un tablero visualmente cerrado, pero no un proyecto verificablemente terminado.
