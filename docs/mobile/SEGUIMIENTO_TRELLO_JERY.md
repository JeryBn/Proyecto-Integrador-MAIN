# MAIN — seguimiento de tarjetas asignadas a Jery

**Actualización posterior:** consultar [estado actual por tarjeta](../ESTADO_IMPLEMENTACION_2026_10_18.md). Las conclusiones anteriores sobre tareas compartidas y alcance se conservan como historial y no describen la implementación nueva.

Consulta del tablero `2026-2-C24-A-SPRINT 1` el 18 de octubre de 2026. Se encontraron 31 tarjetas asignadas a Jery (incluye tarjetas compartidas): 5 ya estaban marcadas terminadas y 26 seguían abiertas. Este documento registra el estado observado; no cambia Trello.

## Ya terminadas en Trello

- T01 — Spring Boot HU06.
- T02 — Spring Boot HU07.
- TÉCNICA — Integración técnica React + Spring Boot (tarjeta sWFFNO9g; no corresponde a T03).
- T07 — Kotlin: listado de actividades pendientes.
- T08 — Kotlin: detalle de actividad.

## Trabajo aplicado en esta revisión

- **M04 — Sistema visual profesional con Material 3:** añadidos temas, tokens visuales y destinos para las seis secciones previstas. Mensajes, Estados y Asistente muestran estados de preparación, sin simular servicios.
- **M05 — Navegación principal y panel académico:** Inicio usa perfil y actividades del servidor autenticado; navegación, Perfil, tema de sesión y salida quedaron conectados.
- **M18 — Documentación, evidencias y cierre:** añadidos el resultado técnico y este seguimiento. La tarjeta sigue abierta hasta tener evidencia de aceptación y cierre del equipo.
- **M23 — Guía de traspaso de app a web:** redactado el flujo de integración; falta revisión del equipo y validación cruzada.
- **M14 — Perfil, ajustes y privacidad:** avance parcial con perfil real, tema de sesión y cierre de sesión. Roles, privacidad y ajustes que requieren servidor siguen pendientes.
- **M17 — APK, teléfono físico y aceptación:** generado APK debug. No equivale a un APK firmado de entrega ni a aceptación en un teléfono físico.
- **JERY — Planificar cliente Kotlin para HU06/HU07:** el código relacionado ya está en la rama base y se conservó; la propia tarjeta deja pendiente confirmar el alcance con el equipo.

Estos avances no modifican por sí solos el estado de las tarjetas. No se marcaron como terminadas.

## Tarjetas abiertas y dependencias

| Tarjeta | Estado observado | Pendiente principal |
|---|---|---|
| EQUIPO — objetivo, alcance y definición de terminado del Sprint 1 (compartida) | Lista de tareas, checklist 0/4 | Acuerdo del equipo y criterios de aceptación. |
| T10 — base compartida y migraciones (Jery + Michael) | Lista de tareas, checklist 0/3 | Acordar propiedad de datos, esquema y migraciones con backend. |
| M22 — API compartida para Android y web (Jery + Michael) | En progreso | Contratos y coordinación de integración entre clientes y API. |
| M18 — documentación, evidencias y cierre (equipo) | En progreso | Revisión del equipo, evidencia de aceptación y cierre. |
| HU06 — validación web del equipo | Pruebas, checklist 0/5 | Validación coordinada con Web y evidencia del equipo. |
| HU07 — validación web del equipo | Pruebas, checklist 0/5 | Validación coordinada con Web y evidencia del equipo. |
| JERY — alcance Kotlin HU06/HU07 | Pruebas, checklist 2/3 | Confirmar alcance con el equipo. |
| M01 — consolidar alcance, historias y entregas | Pendiente | Decisión compartida de alcance y plan. |
| M02 — arquitectura, contratos y persistencia compartida | Pendiente | Contrato y persistencia acordados por el equipo. |
| M03 — usuarios, roles y parentescos verificados | Pendiente | Definición y datos verificados en servidor. |
| M04 — sistema visual Material 3 (Jery + Pamela) | Pendiente | Revisar prototipo y accesibilidad con Web/equipo; ejecutar aceptación en dispositivo. |
| M05 — navegación principal y panel académico (Jery) | Pendiente | Pruebas automatizadas en emulador aprobadas; falta aceptación del equipo y revisión física. |
| M06 — conversaciones privadas y tiempo real (Jery + Michael) | Pendiente | Contrato, autorización y servicio compartido. |
| M07 — grupos privados | Pendiente | Membresía y audiencia verificadas por servidor. |
| M08 — multimedia | Pendiente | Servicio, almacenamiento y permisos compartidos. |
| M09 — estados multimedia | Pendiente | Contrato de publicación, audiencia y caducidad. |
| M10 — notificaciones y sincronización | Pendiente | Servicio común y comportamiento entre plataformas. |
| M11 — definir IA y proveedor | Pendiente | Decisión del equipo y reglas sobre datos/contexto. |
| M12 — integrar asistente IA (Jery) | Pendiente | Depende de M11 y del contrato de servicio. |
| M14 — perfil, ajustes y privacidad (Jery + Pamela) | Pendiente | Completar controles y permisos respaldados por servidor. |
| M16 — servidor compartido y despliegue (Michael + Jery) | Pendiente | Coordinación de entorno y despliegue con responsable de API. |
| M17 — APK, teléfono físico y aceptación (Jery) | Pendiente | Firma de entrega, prueba física y aceptación. |
| M19 — moderación de mensajes y estados | Pendiente | Depende de política y controles del servidor. |
| M20 — políticas de contenido (equipo) | Pendiente | Acuerdo y aprobación compartidos. |
| M23 — guía de traspaso app → web (equipo) | Pendiente | Revisar la guía y validar el flujo con Web. |
| M24 — validar sincronización/paridad (equipo) | Pendiente | Ejecutar con Web y API desplegados, con datos y cuentas de prueba autorizados. |

Las tareas marcadas como pendientes de coordinación no se completan de forma segura desde Android aislado. Los cambios de esta revisión no inventan contratos, roles, datos académicos, políticas ni resultados de aceptación.

## Rama y verificación

Los cambios están en la rama `codex/main-trello-jery`, creada desde `main` en una copia de trabajo. La copia original del repositorio y la rama `main` no se modificaron.

Los bloqueos del emulador y de Mockito quedaron resueltos el 18 de octubre de 2026. La validación final pasó: 29 pruebas del servidor, 15 instrumentadas Android contra ese servidor y 3 unitarias Android, sin fallos, errores ni casos omitidos. También pasaron compilación y lint (0 errores, 5 avisos de versiones); se generó APK debug. Continúan pendientes teléfono físico, firma de entrega, aceptación externa y módulos compartidos que requieren servicios o acuerdos. El procedimiento está en [Verificación local](VERIFICACION_LOCAL.md).
