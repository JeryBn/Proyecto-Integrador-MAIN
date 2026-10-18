# Estado de implementación MAIN — revisión local del 18/10/2026

Esta revisión sustituye las conclusiones de alcance y los recuentos anteriores de docs/mobile. No modifica por sí misma el estado de ninguna tarjeta de Trello. Las tareas compartidas se implementaron dentro de los contratos del repositorio; no se presume aceptación de Michael, Pamela ni del profesor.

## Funciones disponibles
- Identidades persistentes ALUMNO/DOCENTE/FAMILIAR, asignación administrativa, contraseñas con hash, recuperación administrativa y cierre de sesiones revocadas.
- Parentescos verificados, cursos y horarios según permisos; preferencias persistentes y contactos autorizados.
- Chat de texto con grupos, invitación/aceptación, roles y salida; paginación y sincronización mientras la conversación está abierta.
- Revisión humana antes de publicar, versiones inmutables, hash, protección contra revisión desactualizada y separación de corrección previa/edición posterior.
- Buzón persistente de avisos aprobados e invitaciones; leído, preferencias, silencios, bloqueos y revocación.
- Asistente académico Android-servidor-OpenAI, contexto mínimo autorizado, límite de consumo, errores, cancelación local y reintento explícito.
- Migraciones Flyway V1–V7, perfil local persistente, perfil de producción propuesto, firma Android configurable mediante variables externas.
- Pantalla administrativa de revisión separada del frontend React existente.

## Estado por tarjeta
| Tarjeta | Implementación o evidencia | Pendiente para aceptación completa |
|---|---|---|
| M01 / EQUIPO / JERY alcance | Inventario y asignaciones originales conservados; este estado distingue verificado/pendiente | Estimaciones y aceptación conjunta; no publicar porcentaje global sin esfuerzo aceptado |
| M02 / T10 | Fuente actual Spring, usuarios/datos/migraciones persistentes, reinicio local probado | MySQL real y acuerdo con Michael; no existe implementación Django en este checkout |
| M03 | Roles, parentesco verificado y revocado, acceso y recuperación administrativa | Datos reales, flujo operativo del administrador y validación del equipo |
| M04 | Material3, navegación, tema, pantallas funcionales | Validación visual del usuario, más tamaños/fuentes y dispositivo físico |
| M05 | Cursos/horarios reales por autorización; datos vacíos sin inventar | Cargar horarios reales y aceptación académica |
| M06 | Mensajes de texto, revisión, corrección, paginación, reintento sin duplicar | Pruebas prolongadas de conexión y recorrido con usuarios reales |
| M07 | Grupos, invitaciones, aceptación, administración, salida/revocación | Revisión funcional del equipo |
| M08 | No implementado | Subida y validación real de imágenes/video/audio, almacenamiento privado y UI |
| M09 | Estados Android/API de texto, imagen, video y audio; audiencia, 24 h, vistas, borrado y revisión humana | Retención/purga, apelaciones, automatización multimodal y validación en teléfono físico; detalles en NOTAS_DOCENTES_Y_ESTADOS_2026_10_18.md |
| M10 | Buzón persistente de avisos y consulta Android | Push/segundo plano, recordatorios y pruebas de entrega con app cerrada |
| M11 | Proveedor OpenAI conectado para tutor, límites y secretos solo servidor | Saldo del proyecto y moderación multimodal automática |
| M12 | Pantalla IA, consulta real al servidor, cancelación y errores comprobables | Obtener respuesta generada con saldo; evaluación pedagógica y casos reales |
| M14 | Preferencias, contactos, silencio, bloqueo y reporte | Avatar y avisos del sistema |
| M16 | Perfil de producción y propuesta Caddy HTTPS | Hosting/dominio, DB TLS real, respaldo/restauración y dos dispositivos |
| M17 | Compilación release exige firma externa completa | Crear/custodiar firma, APK de entrega y teléfono físico |
| M18 | Código, pruebas, documentos y entrega local | Aceptación de revisión y publicación acordada |
| M19 | Cola manual y hash/revisión exacta; sin aprobación por ausencia de IA | Automatización multimodal, recuperación de cola y apelaciones |
| M20 | Categorías iniciales aprobadas por Jery | Revisor de apelaciones y plazo de conservación; aún no hay apelaciones implementadas |
| M22 | Contratos backend v1 implementados y probados | Validación conjunta de contratos nuevos con web |
| M23 | Documentación actual y guía local de pruebas | Traspaso y aceptación del equipo |
| M24 / HU06 / HU07 | Contratos antiguos conservados; Android verificado | Validación web-Android actual con la versión de Pamela y mismos datos |
| TÉCNICA, T01, T02, T07, T08 | Ya aparecían terminadas en Trello | No se atribuyen como tarjetas nuevas completadas |

M13, M15 y M21, asignadas exclusivamente a otros integrantes, no se reclaman como trabajo terminado. Las pruebas necesarias para comprobar nuestros cambios sí se ejecutan.

## Bloqueos que requieren al usuario
1. OpenAI: la llamada real respondió credit_balance_exhausted / insufficient_quota. Activar saldo o revisar facturación del proyecto seleccionado. No hace falta crear otra clave.
2. Moderación: indicar responsable de apelaciones y plazo de conservación. Las categorías iniciales ya están aprobadas.
3. Producción: elegir hosting y dominio, facilitar acceso de despliegue y una DB de prueba autorizada, con presupuesto si corresponde.
4. Entrega Android: destino de custodia/backup de la firma y teléfono USB con depuración cuando se vaya a validar.
5. Integración del equipo: branch/commit web vigente y acuerdo sobre migraciones/propiedad de datos. No se migró ni borró ninguna DB ajena.

M08 y las demás partes faltantes son trabajo de implementación pendiente; no se presentan como bloqueadas solo por falta de una decisión del usuario.

## Incremento de notas y estados

Registro/edición de notas por el docente desde Android, matrícula y curso comprobados en el servidor, consulta del alumno y detección de edición simultánea en el flujo móvil. Estados y límites documentados en `NOTAS_DOCENTES_Y_ESTADOS_2026_10_18.md`. Los adjuntos de chats M08 siguen pendientes.

Verificación de este incremento: 80 pruebas de servidor, 6 recorridos de interfaz Android y 3 pruebas básicas de configuración aprobadas; lint sin errores. Evidencias y límites en el documento del incremento. No se presume validación física ni aceptación de las tarjetas.
