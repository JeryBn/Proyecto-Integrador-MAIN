# MAIN — coordinación de app, web y backend

Actualizado: 12/10/2026. Requisitos explícitos de Jery.

## Estado real

Android HU06/HU07 está integrado en main bajo mobile/. Chats, grupos, multimedia, estados e IA están planificados, todavía no implementados. El 90% anterior no representa avance global; 15–20% es una apreciación provisional sin estimaciones completas.

## Fuente de coordinación

Tablero: https://trello.com/b/vrcqu5di/2026-2-c24-a-sprint-1
Lista BACKLOG MAIN COMPLETO: 24 tarjetas con miembros asignados y vencimiento 20/10/2026 23:59. M01–M18: producto y entrega; M19–M20: moderación; M21–M24: web y coordinación. Responsabilidades asignadas por Jery para el 20/10/2026; ver ASIGNACION_SEMANA_20_OCT.md; se preservan T09/T10/T11.

## Reglas comunes obligatorias

- La web refleja las funciones y reglas de la app usando contratos, identidad y datos compartidos. El servidor aplica permisos en ambas plataformas.
- Solo estudiantes y docentes publican estados. Familiares/tutores no publican; lectura depende de audiencia autorizada.
- Moderación previa de texto, imágenes, video y audio para todos los emisores en chats individuales/grupos y estados.
- Pendiente/rechazado queda privado. Editar exige revisar la nueva versión. Si falla la IA o el resultado es incierto, no se entrega/publica automáticamente.
- Tras corregir un rechazo, receptor ve solo la versión aprobada y «Editado antes del envío para cumplir las normas de MAIN». No ve original rechazado.
- Definir normas, contexto educativo, apelación, retención y proveedor con M20/M11. IA no garantiza detección perfecta.
- Asistente académico y moderación son funciones distintas. El asistente no accede automáticamente a chats privados.

## Orden de trabajo

1. M01/M02: historias, contratos y modelo compartido con T10.
2. M03: identidad/roles/vínculos; M04 en paralelo: diseño profesional Material 3.
3. M05/M06: navegación y comunicación real; diseñar M20/M19 desde el contrato, implementar moderación antes de cerrar entrega de mensajes.
4. M07/M08/M09: grupos, archivos y estados con permisos/revisión.
5. M11/M12: asistente IA; M10/M14: notificaciones/ajustes.
6. M13/M15/M16/M17/M18: integración, calidad, despliegue y aceptación. Documentación/pruebas se actualizan en cada avance.

Asignación de Jery: Jery app y traspaso; Pamela interfaz web; Michael administración/backend/pruebas. Pueden avanzar en contratos, persistencia y componentes web antes de terminar toda la app. No esperar al final para integrar.

## Matriz de seguimiento

| Módulo | Android | Web | Contrato/servidor |
|---|---|---|---|
| Acceso y actividades HU06/HU07 | Implementado/probado en emulador | Base existente; revisión independiente pendiente | Base Spring existente |
| Identidad persistente y familiares | Pendiente | Pendiente | M03 |
| Diseño completo y agenda | Pendiente | Pendiente | M04/M05 y historias M01 |
| Chats y grupos | Pendiente | Pendiente | M06/M07/M19 |
| Medios y estados | Pendiente | Pendiente | M08/M09/M19 |
| Asistente IA | Pendiente | Pendiente | M11/M12 |
| Notificaciones/perfil/ajustes | Pendiente | Pendiente | M10/M14 |

## Cierre por incremento

Actualizar main; revisar contrato y versión real de app; implementar aporte propio; probar recorridos positivos/negativos; commit separado y descriptivo por tarea terminada; enlazar commit, resultados y capturas en Trello. Registrar dependencias, defectos y cambios de contrato. Prueba aprobada no sustituye aceptación del equipo. No guardar claves/tokens/contraseñas personales en repositorio ni APK.

Prueba cruzada mínima: enviar Android→web y web→Android, usar mismas cuentas/datos, verificar archivos y grupos, impedir publicación familiar desde ambas plataformas y demostrar que contenido pendiente/rechazado no se recibe.

## Tarjetas de coordinación

### M21 [Web/Equipo] Paridad funcional web con la app MAIN

https://trello.com/c/nWNxf793/39-m21-web-equipo-paridad-funcional-web-con-la-app-main

Coordinación: Pamela interfaz web; Michael administración/backend/pruebas según acuerdo del equipo. Jery mantiene app. Fuente funcional: app MAIN y contratos compartidos; no copiar implementación Android. Depende de cada incremento móvil aceptado y M02/M03/M19/M20.

Criterios:
- Matriz por módulo APP/WEB/API: acceso/perfil, actividades/agenda, mensajes individuales, grupos, medios, estados, IA, notificaciones y ajustes. Registrar pendiente/en desarrollo/probado por plataforma.
- Web reproduce reglas: estados solo alumno/docente; familiares no publican; moderación previa para todo emisor; edición rechazada se revisa otra vez; receptor solo recibe versión aprobada y su marca.
- Usar mismo servidor, identidad, relaciones, audiencias y datos; ninguna regla de seguridad depende solo de esconder botones.
- UX web adaptable con identidad visual MAIN, colores/tipografía/componentes equivalentes; navegación adaptada a escritorio.
- Por cada módulo: contrato versionado, captura/video móvil real y estados de carga/vacío/error antes de declarar paridad. Una pantalla aún planificada no se presenta como implementada.

### M22 [Backend/Equipo] API compartida para Android y web

https://trello.com/c/q2Ewnesn/40-m22-backend-equipo-api-compartida-para-android-y-web

Coordinar M02 con T09/T10/T11 existentes. Responsables del servidor y migraciones se acuerdan con equipo; no se modifica su autoría.

Criterios:
- Documentar endpoints, DTO, roles, errores, paginación, eventos, CSRF/sesión, archivos y moderación; ejemplos sanitizados sin credenciales personales.
- Aplicar en servidor vínculos familiares, membresía de grupos, audiencia/expiración y revisión previa de contenido.
- Compartir identidades y datos entre plataformas; acordar autoridad de Django/Spring por dominio y dueño de migraciones.
- Contratos compatibles/versionados; avisar en tarjeta y actualizar guía antes de cambios que rompan app/web.
- Cuenta de prueba en Android y web ve los mismos datos; mensajes/estados aprobados sincronizan y pendientes/rechazados no se filtran.

### M23 [Jery + Equipo] Guía de traspaso app a web por incremento

https://trello.com/c/pE3vpWEA/41-m23-jery-equipo-gu%C3%ADa-de-traspaso-app-a-web-por-incremento

Jery publica referencia móvil; compañeros implementan web según reparto acordado. Depende del incremento real de cada módulo.

Guía de trabajo:
1. Clonar/actualizar main; app está en mobile/. Base HU06/HU07 ya implementada; chats/estados/IA todavía pendientes.
2. Revisar contrato del módulo y capturas reales; contrastar requisitos M01–M22 con implementación.
3. Registrar dudas/dependencias en su tarjeta; no duplicar bases, usuarios ni moderación.
4. Implementar web con mismos campos, permisos, estados de error y resultado funcional.
5. Probar junto a app con las mismas cuentas y servidor; aportar evidencia y commit propio.
6. Actualizar matriz y Trello; cerrar solo criterios verificados, aceptación por equipo aparte.

Criterios:
- Por módulo, publicar rutas/pantallas, contrato y ejemplo de datos, reglas de permisos y casos positivos/negativos.
- Diseño: compartir paleta, tipografía, componentes y comportamiento responsive; identificar versión exacta de app/commit.
- Dependencias y cambios pendientes visibles; no prometer funciones futuras como ya disponibles.

### M24 [Pruebas/Equipo] Validar sincronización y paridad Android-web

https://trello.com/c/TiQ3ZIys/42-m24-pruebas-equipo-validar-sincronizaci%C3%B3n-y-paridad-android-web

Propuesta: Michael coordina pruebas independientes; Jery/Pamela apoyan sus plataformas. Validación conjunta pendiente. Depende de M21/M22 y módulos implementados.

Criterios:
- Enviar desde Android y recibir web, y viceversa, con texto/medios/grupos y persistencia tras reinicio.
- Estado aprobado en una plataforma aparece a audiencia autorizada en otra; familiar no publica desde ninguna API/cliente.
- Contenido rechazado/pending no llega a otra plataforma ni sus notificaciones; edición aprobada muestra la misma marca.
- Membresía revocada, usuario ajeno, cambio de cuenta, reconexión y proveedor IA caído dan resultados coherentes.
- Registrar versiones, cuentas sintéticas, resultados y defectos; adjuntar evidencia y aceptación separada del equipo.

