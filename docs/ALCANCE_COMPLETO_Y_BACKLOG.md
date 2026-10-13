# MAIN — backlog completo y asignaciones

Fecha objetivo solicitada por Jery: martes 20/10/2026 23:59. La asignación siguiente reemplaza las propuestas anteriores de reparto. Historial de fuentes: HU06/HU07 visibles en enlace compartido; nuevos módulos y normas vienen de solicitudes explícitas de Jery. Funciones futuras pendientes, no declarar implementadas por estar en Trello.

## M01 [Equipo] Consolidar alcance, historias y plan de entregas

https://trello.com/c/eqleey71/19-m01-equipo-consolidar-alcance-historias-y-plan-de-entregas

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe, Michael Julian Fernandez Camarena, Pamela Regina Meneses Mayhua.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Pamela: implementación o referencia web del módulo, integración React, estados de pantalla y evidencia de funcionamiento.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de: ninguna. Coordina Jery; validación conjunta pendiente. Separar visión completa de Sprint 1. Fuente: requisitos explícitos de Jery del 12/10/2026; enlace compartido visible solo confirma planificación HU06/HU07.

Criterios:
- [ ] Inventariar historias de academia, IA, mensajes, grupos, estados y perfiles, con dos criterios verificables por historia.
- [ ] Registrar prioridades, dependencias, estimaciones del equipo y responsabilidades acordadas.
- [ ] Calcular avance por esfuerzo aceptado; retirar 90% como avance global (15–20% es solo apreciación provisional).

## M02 [Equipo] Arquitectura, contratos y persistencia compartida

https://trello.com/c/RnkwF0eI/20-m02-equipo-arquitectura-contratos-y-persistencia-compartida

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe, Michael Julian Fernandez Camarena.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M01; coordinar con T10 existente, sin duplicar su implementación. Propuesta conjunta Jery + Michael.

Criterios:
- [ ] Definir fuente única de usuarios, roles, cursos, parentescos, conversaciones, mensajes, medios y estados.
- [ ] Versionar contratos API, errores, paginación, migraciones y permisos entre Spring Boot, Django, Kotlin y React.
- [ ] Probar persistencia tras reiniciar y acordar entornos local/pruebas/servidor compartido.

## M03 [App/API] Usuarios reales, roles y parentescos verificados

https://trello.com/c/T7k14Jij/21-m03-app-api-usuarios-reales-roles-y-parentescos-verificados

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe, Michael Julian Fernandez Camarena.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M02. Jery: app; servidor y administración: acuerdo del equipo. Parentescos se interpreta como familiares/tutores vinculados al alumno.

Criterios:
- [ ] Acceso y perfil para alumno, docente y familiar/tutor; usuarios persistidos y recuperación de acceso definida.
- [ ] Relación familiar-alumno validada por flujo administrativo, sin autootorgar acceso.
- [ ] Probar matriz de autorización, sesión expirada y separación de datos entre cuentas.

## M04 [Jery/App] Sistema visual profesional con Material 3

https://trello.com/c/PQFqc1Dh/22-m04-jery-app-sistema-visual-profesional-con-material-3

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe, Pamela Regina Meneses Mayhua.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Pamela: implementación o referencia web del módulo, integración React, estados de pantalla y evidencia de funcionamiento.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M01; puede avanzar mientras se acuerda M02.

Criterios:
- [ ] Definir colores, tipografía, espaciado, iconos, componentes y temas claro/oscuro con Material 3.
- [ ] Prototipar inicio, actividades, mensajes, estados, IA y perfil; adaptar a pantalla pequeña/grande y accesibilidad.
- [ ] Aplicar diseño al acceso/listado/detalle existentes sin perder navegación ni manejo de errores.

## M05 [Jery/App] Navegación principal y panel académico

https://trello.com/c/naY89EwE/23-m05-jery-app-navegaci%C3%B3n-principal-y-panel-acad%C3%A9mico

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M03/M04; reutiliza T07/T08.

Criterios:
- [ ] Navegación entre Inicio, Actividades, Mensajes, Estados, IA y Perfil con estado conservado.
- [ ] Mostrar agenda/horario e información académica autorizada según historias de M01; sin datos ficticios presentados como reales.
- [ ] Comprobar carga, vacío, error, reintento y cambio de cuenta en cada recorrido.

## M06 [App/API] Conversaciones privadas y mensajes en tiempo real

https://trello.com/c/ayO8UuLc/24-m06-app-api-conversaciones-privadas-y-mensajes-en-tiempo-real

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe, Michael Julian Fernandez Camarena.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M02/M03/M04/M19/M20. Jery: app; coordinación de contrato/servidor con equipo.

Criterios:
- Conversaciones persistidas, texto, paginación y reconexión entre contactos autorizados. Seis parejas de roles según matriz acordada.
- TODO emisor, incluido docente y familiar, pasa revisión de mensajes/adjuntos antes de entrega. Estados pendientes/rechazados no se envían al receptor ni generan notificaciones con ese contenido.
- Rechazo explica la categoría de la norma al emisor y permite editar o cancelar. La versión editada se analiza otra vez; solo se entrega cuando es aprobada.
- Si se corrigió tras un rechazo, mostrar al receptor: «Editado antes del envío para cumplir las normas de MAIN». No exponer original rechazado ni historial privado de moderación.
- Distinguir esta marca de una edición normal después del envío. Probar doble envío, desconexión, revisión no disponible e intento de evadir API.

## M07 [App/API] Grupos privados, miembros y administración

https://trello.com/c/C01A7tfK/25-m07-app-api-grupos-privados-miembros-y-administraci%C3%B3n

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe, Michael Julian Fernandez Camarena.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M06 y matriz M03.

Criterios:
- [ ] Crear grupos de alumnos/docentes/familiares, mixtos o del mismo rol, incluidos alumno-docente-familiar y docente-familiar.
- [ ] Invitaciones, aceptación, administradores, salida y expulsión controladas por servidor.
- [ ] Solo miembros acceden; retirada de miembro revoca lectura/envío futuros según política documentada.

## M08 [App/API] Imágenes, videos con audio y archivos en mensajes

https://trello.com/c/bO7wJrgY/26-m08-app-api-im%C3%A1genes-videos-con-audio-y-archivos-en-mensajes

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe, Michael Julian Fernandez Camarena.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M06/M07; almacenamiento acordado en M02.

Criterios:
- [ ] Seleccionar y enviar imágenes y videos con audio; definir documentos/notas de voz en M01.
- [ ] Validar formato/tamaño en servidor, mostrar progreso, cancelar/reintentar y reproducir medios.
- [ ] URLs/descargas autorizadas por conversación; probar fallo de carga, archivo inválido y acceso ajeno.

## M09 [App/API] Estados multimedia y privacidad de audiencia

https://trello.com/c/SFeqNex6/27-m09-app-api-estados-multimedia-y-privacidad-de-audiencia

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe, Michael Julian Fernandez Camarena.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M03/M08/M19/M20. Requisito explícito de Jery, 12/10/2026.

Criterios:
- Solo alumnos y docentes pueden crear/publicar estados. Familiares/tutores NO pueden publicar; el servidor debe rechazar el intento incluso fuera de la app. Su permiso de visualización se define por audiencia, no se presume acceso a todo.
- Imágenes y videos con audio pasan moderación antes de publicarse. Se revisan imagen/video, texto visible y audio, no solo el nombre del archivo.
- Un estado pendiente o rechazado no aparece en feeds ni puede descargarse por terceros. Editar/sustituir contenido exige nueva revisión.
- Definir duración (24 h como propuesta), audiencia, visualizaciones, eliminación y retención.
- Probar publicación alumno/docente, rechazo familiar, contenido inapropiado, revisión no disponible y acceso ajeno.

## M10 [App/API] Notificaciones y sincronización fiable

https://trello.com/c/Sp0H59Ki/28-m10-app-api-notificaciones-y-sincronizaci%C3%B3n-fiable

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe, Michael Julian Fernandez Camarena.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M05/M06/M07/M09. Complemento propuesto, no requisito confirmado en enlace.

Criterios:
- [ ] Notificar mensajes, invitaciones y recordatorios configurables con permiso del dispositivo.
- [ ] Abrir destino correcto y actualizar contadores sin duplicar mensajes al reconectar.
- [ ] No mostrar contenido privado de otra cuenta; comprobar cierre de sesión y permisos denegados.

## M11 [Equipo/IA] Definir asistente académico y proveedor

https://trello.com/c/NdK02gB6/29-m11-equipo-ia-definir-asistente-acad%C3%A9mico-y-proveedor

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe, Michael Julian Fernandez Camarena.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M01/M02/M03/M20. Dos funciones separadas: asistente académico y moderación obligatoria previa a publicación/envío (M19).

Criterios:
- Definir casos de uso del asistente, proveedor local/remoto, costes, disponibilidad y contrato; secretos fuera de APK.
- Elegir y evaluar detección de texto, imágenes, videos y audio para moderación; no asumir que un modelo conversacional cubre todos los medios.
- Revisar tratamiento/retención de datos antes de usar proveedor externo; enviar solo el contenido necesario y contexto acotado.
- Documentar falsos positivos/negativos, incertidumbre, categorías y pruebas. Una clasificación de IA no garantiza seguridad absoluta.
- El asistente no obtiene acceso automático a chats privados por existir moderación.

## M12 [Jery/App + API] Integrar asistente IA funcional

https://trello.com/c/7Qn9shZI/30-m12-jery-app-api-integrar-asistente-ia-funcional

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M11/M05. Moderación previa se implementa en M19, con permisos y contrato propios.

Criterios:
- Pantalla del asistente conectada al proveedor real mediante backend, conversación, espera, cancelar y reintentar.
- Contexto académico autorizado y fuentes visibles cuando corresponda; no lectura general de chats privados.
- Probar indisponibilidad, límites, respuestas incorrectas y aislamiento por cuenta.
- No declarar completada por un mock ni confundir ayuda académica con la moderación obligatoria de mensajes/estados.

## M13 [Equipo] Administración e integración web con app

https://trello.com/c/OaZqYFzL/31-m13-equipo-administraci%C3%B3n-e-integraci%C3%B3n-web-con-app

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Michael Julian Fernandez Camarena, Pamela Regina Meneses Mayhua.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Pamela: implementación o referencia web del módulo, integración React, estados de pantalla y evidencia de funcionamiento.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M02/M03/M07/M09; vincular T09/T11 existentes. Trabajo de Michael/Pamela sujeto a acuerdo.

Criterios:
- [ ] Administrar usuarios, vinculaciones, cursos y pertenencias con permisos definidos.
- [ ] Moderación/reportes y auditoría de cambios necesarios para comunicación privada.
- [ ] Crear datos desde administración y comprobarlos en app sin duplicar modelos ni romper HU06/HU07.

## M14 [App/API] Perfil, ajustes y controles de privacidad

https://trello.com/c/VI8J36fp/32-m14-app-api-perfil-ajustes-y-controles-de-privacidad

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe, Pamela Regina Meneses Mayhua.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Pamela: implementación o referencia web del módulo, integración React, estados de pantalla y evidencia de funcionamiento.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M03/M04/M06/M09.

Criterios:
- [ ] Perfil/avatar, ajustes de tema, notificaciones y audiencia de estados según permisos.
- [ ] Silenciar/bloquear/reportar contactos y definir efecto en chats/grupos con servidor.
- [ ] Cierre de sesión elimina datos privados locales; permisos multimedia se solicitan al usar la función.

## M15 [Equipo] Pruebas completas, seguridad y rendimiento

https://trello.com/c/JjlG2EqD/33-m15-equipo-pruebas-completas-seguridad-y-rendimiento

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Michael Julian Fernandez Camarena.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de incrementos M03–M14; probar también durante cada tarea. Vincular revisión T05/T06.

Criterios:
- [ ] Pruebas de contrato, integración y UI con varias cuentas/roles, grupos privados, estados, IA y regresión HU06/HU07.
- [ ] Comprobar permisos por objeto, CSRF/sesión, acceso a archivos y exposición de secretos.
- [ ] Probar carga/paginación, reconexión, multimedia y dispositivo físico; guardar resultados y corregir fallos.

## M16 [Equipo] Servidor compartido, despliegue y operación

https://trello.com/c/tWfZ3s43/34-m16-equipo-servidor-compartido-despliegue-y-operaci%C3%B3n

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Michael Julian Fernandez Camarena, Jery Becerra Ninaquispe.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M02 y servicios implementados; decisiones de infraestructura del equipo.

Criterios:
- [ ] Entorno compartido con HTTPS, base persistente, almacenamiento de medios y configuración fuera del código.
- [ ] Respaldos/restauración, registros sin datos sensibles y seguimiento de errores.
- [ ] Dos dispositivos intercambian mensajes y ven datos reales del mismo entorno; verificar disponibilidad de IA.

## M17 [Jery/App] APK de entrega, teléfono físico y aceptación

https://trello.com/c/OipuXCub/35-m17-jery-app-apk-de-entrega-tel%C3%A9fono-f%C3%ADsico-y-aceptaci%C3%B3n

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M15/M16 y alcance acordado.

Criterios:
- [ ] Generar versión firmada de entrega con configuración correcta; custodiar firma fuera del repositorio.
- [ ] Instalar y demostrar recorridos completos en teléfono físico y emulador.
- [ ] Equipo registra aceptación por historia y pendientes; no confundir build aprobado con aceptación.

## M18 [Equipo] Documentación, evidencias y cierre del proyecto

https://trello.com/c/8EMiZ9A7/36-m18-equipo-documentaci%C3%B3n-evidencias-y-cierre-del-proyecto

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe, Michael Julian Fernandez Camarena, Pamela Regina Meneses Mayhua.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Pamela: implementación o referencia web del módulo, integración React, estados de pantalla y evidencia de funcionamiento.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Se actualiza por incremento; cierre tras M17.

Criterios:
- [ ] Manual usuario/técnico, arquitectura, contratos, migraciones, decisiones IA y guía de instalación actualizados.
- [ ] Commit separado por tarea terminada en main, pruebas/capturas y enlace en Trello; conservar autoría.
- [ ] Entrega reproducible, inventario de alcance y limitaciones, revisión final de rúbrica y demostración del equipo.

## M19 [App/API/IA] Moderación previa de mensajes y estados

https://trello.com/c/qeft1Wx1/37-m19-app-api-ia-moderaci%C3%B3n-previa-de-mensajes-y-estados

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe, Michael Julian Fernandez Camarena.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Depende de M02/M03/M08/M11/M20. Requisito de Jery, 12/10/2026.

Flujo: borrador → pendiente de revisión → aprobado y entregado/publicado; o rechazado → editar/cancelar → nueva revisión. Error técnico o resultado incierto: pendiente, nunca aprobación automática.

Criterios:
- Analizar texto, imágenes, video y audio/transcripción en mensajes individuales, grupos y estados. Definir cobertura del video y límites del análisis.
- Autorizar en servidor exactamente la versión revisada; cualquier cambio invalida la aprobación. Cliente no decide aprobar/publicar.
- Pendientes y rechazados quedan privados: no aparecen en feed, eventos, descargas ni notificaciones al receptor.
- Rechazo explica la norma y ofrece editar/cancelar. La edición pasa otra revisión. Solo entregar versión aprobada.
- Mostrar «Editado antes del envío para cumplir las normas de MAIN» cuando hubo corrección tras rechazo; ocultar original rechazado. Distinguir edición posterior al envío.
- Cola, reintentos, idempotencia, revocación de permisos y pruebas de caída del proveedor, acceso ajeno y bypass API.
- IA no garantiza detección perfecta; casos inciertos/reclamaciones usan revisión humana autorizada definida en M20.

## M20 [Equipo] Normas de contenido y evaluación de moderación

https://trello.com/c/gF9ubXm5/38-m20-equipo-normas-de-contenido-y-evaluaci%C3%B3n-de-moderaci%C3%B3n

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe, Michael Julian Fernandez Camarena, Pamela Regina Meneses Mayhua.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Pamela: implementación o referencia web del módulo, integración React, estados de pantalla y evidencia de funcionamiento.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Precede configuración de M19. Requisitos de Jery: sin contenido sexual ni conductas inadecuadas para estudiantes.

Criterios:
- Definir categorías y ejemplos. Propuesta: sexual explícito, acoso, amenazas, insultos dirigidos, violencia gráfica, discriminación y promoción de conductas peligrosas. Validar reglas detalladas con equipo.
- Distinguir contexto educativo/salud apropiado de sexual explícito, denuncia de acoso de acoso y citas de promoción.
- Evaluar falsos positivos/negativos en texto español y medios; prever apelación y revisión humana autorizada sin difundir originales rechazados.
- Definir retención/borrado, acceso a auditoría y tratamiento de datos antes de enviar contenido a proveedor externo.
- Publicar estados solo alumno/docente; familiares NO publican. Lectura según audiencia. Moderación de mensajes aplica a TODOS los emisores.
- Aprobar reglas del producto y registrar limitaciones; no prometer contenido seguro al 100%.

## M21 [Web/Equipo] Paridad funcional web con la app MAIN

https://trello.com/c/nWNxf793/39-m21-web-equipo-paridad-funcional-web-con-la-app-main

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Pamela Regina Meneses Mayhua.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Pamela: implementación o referencia web del módulo, integración React, estados de pantalla y evidencia de funcionamiento.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Coordinación: Pamela interfaz web; Michael administración/backend/pruebas según la asignación de Jery. Jery mantiene app. Fuente funcional: app MAIN y contratos compartidos; no copiar implementación Android. Depende de cada incremento móvil aceptado y M02/M03/M19/M20.

Criterios:
- Matriz por módulo APP/WEB/API: acceso/perfil, actividades/agenda, mensajes individuales, grupos, medios, estados, IA, notificaciones y ajustes. Registrar pendiente/en desarrollo/probado por plataforma.
- Web reproduce reglas: estados solo alumno/docente; familiares no publican; moderación previa para todo emisor; edición rechazada se revisa otra vez; receptor solo recibe versión aprobada y su marca.
- Usar mismo servidor, identidad, relaciones, audiencias y datos; ninguna regla de seguridad depende solo de esconder botones.
- UX web adaptable con identidad visual MAIN, colores/tipografía/componentes equivalentes; navegación adaptada a escritorio.
- Por cada módulo: contrato versionado, captura/video móvil real y estados de carga/vacío/error antes de declarar paridad. Una pantalla aún planificada no se presenta como implementada.

## M22 [Backend/Equipo] API compartida para Android y web

https://trello.com/c/q2Ewnesn/40-m22-backend-equipo-api-compartida-para-android-y-web

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Michael Julian Fernandez Camarena, Jery Becerra Ninaquispe.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Coordinar M02 con T09/T10/T11 existentes. Responsables del servidor y migraciones se acuerdan con equipo; no se modifica su autoría.

Criterios:
- Documentar endpoints, DTO, roles, errores, paginación, eventos, CSRF/sesión, archivos y moderación; ejemplos sanitizados sin credenciales personales.
- Aplicar en servidor vínculos familiares, membresía de grupos, audiencia/expiración y revisión previa de contenido.
- Compartir identidades y datos entre plataformas; acordar autoridad de Django/Spring por dominio y dueño de migraciones.
- Contratos compatibles/versionados; avisar en tarjeta y actualizar guía antes de cambios que rompan app/web.
- Cuenta de prueba en Android y web ve los mismos datos; mensajes/estados aprobados sincronizan y pendientes/rechazados no se filtran.

## M23 [Jery + Equipo] Guía de traspaso app a web por incremento

https://trello.com/c/pE3vpWEA/41-m23-jery-equipo-gu%C3%ADa-de-traspaso-app-a-web-por-incremento

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Jery Becerra Ninaquispe, Michael Julian Fernandez Camarena, Pamela Regina Meneses Mayhua.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Pamela: implementación o referencia web del módulo, integración React, estados de pantalla y evidencia de funcionamiento.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

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

## M24 [Pruebas/Equipo] Validar sincronización y paridad Android-web

https://trello.com/c/TiQ3ZIys/42-m24-pruebas-equipo-validar-sincronizaci%C3%B3n-y-paridad-android-web

ASIGNACIÓN PARA LA PRÓXIMA SEMANA — dispuesta por Jery como encargado del grupo.
Responsables: Michael Julian Fernandez Camarena, Pamela Regina Meneses Mayhua, Jery Becerra Ninaquispe.
Vencimiento: martes 20/10/2026, 23:59 (hora de planificación).
Michael: servidor/datos/administración y pruebas de integración según el módulo; contrato documentado para clientes.
Pamela: implementación o referencia web del módulo, integración React, estados de pantalla y evidencia de funcionamiento.
Jery: implementación Android, recorrido visible, pruebas móviles y contrato necesario para integrar.
Entrega: commit propio, resultado de pruebas y captura/demostración; registrar avances y dependencias en esta tarjeta. Los miembros asignados coordinan sus partes; aceptación del profesor/equipo no se presume.

Propuesta: Michael coordina pruebas independientes; Jery/Pamela apoyan sus plataformas. Validación conjunta pendiente. Depende de M21/M22 y módulos implementados.

Criterios:
- Enviar desde Android y recibir web, y viceversa, con texto/medios/grupos y persistencia tras reinicio.
- Estado aprobado en una plataforma aparece a audiencia autorizada en otra; familiar no publica desde ninguna API/cliente.
- Contenido rechazado/pending no llega a otra plataforma ni sus notificaciones; edición aprobada muestra la misma marca.
- Membresía revocada, usuario ajeno, cambio de cuenta, reconexión y proveedor IA caído dan resultados coherentes.
- Registrar versiones, cuentas sintéticas, resultados y defectos; adjuntar evidencia y aceptación separada del equipo.
