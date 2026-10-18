# Historias y plan de aceptación — propuesta para M01

Inventario para revisión del equipo; no representa aceptación ni estimaciones acordadas.
Responsabilidades detalladas en ALCANCE_COMPLETO_Y_BACKLOG.md y ASIGNACION_SEMANA_20_OCT.md.
P0 precede a publicación compartida; P1 completa la experiencia; P2 prepara entrega y traspaso.
S/M/L son tamaños técnicos provisionales, no horas comprometidas.

| Historia | Prioridad/tamaño | Criterio verificable 1 | Criterio verificable 2 | Dependencias |
|---|---|---|---|---|
| Como usuario ingreso con mi rol y conservo mi identidad | P0/M | Contraseña incorrecta no inicia sesión ni reutiliza otra cuenta | Inhabilitar/restablecer acceso invalida sesiones anteriores | M02/M03 |
| Como familiar consulto solo alumnos vinculados | P0/M | Sin vínculo verificado no aparecen datos del alumno | Revocar vínculo retira acceso a agenda y al contexto IA | M03 |
| Como alumno/docente consulto mi agenda | P1/M | Solo cursos matriculados/asignados son visibles | Horarios inválidos o solapados son rechazados por administración | M05 |
| Como usuario envío texto privado | P0/L | Pendiente/rechazado es visible solo al autor y revisor autorizado | Retry con misma clave no duplica y una clave con otro contenido falla | M06/M19 |
| Como usuario corrijo un texto rechazado | P0/M | Cada cambio produce revisión/hash nuevos y exige aprobación actual | El receptor solo recibe la versión aprobada y su marca de corrección | M06/M19 |
| Como integrante participo en grupos | P1/L | Invitado no lee hasta aceptar y no ve historial anterior al ingreso | Miembro removido pierde acceso; último administrador debe designar sucesor | M07 |
| Como usuario comparto medios privados | P0/L | Servidor comprueba formato real, tamaño y propiedad | Medio pendiente/rechazado no tiene URL pública ni descarga ajena | M08/M19 |
| Como alumno/docente publico un estado revisado | P1/L | Familiar no puede publicar aunque invoque directamente la API | Audiencia, expiración y aprobación se cumplen en lectura y descarga | M09/M19 |
| Como usuario recibo avisos apropiados | P1/L | Solo contenido aprobado genera aviso y no se duplica | Preferencias, silencio, bloqueo y revocación se respetan al entregar/leer | M10 |
| Como usuario consulto al tutor IA | P1/L | Consulta usa backend y contexto autorizado, sin chats ni clave en APK | Cuota, timeout y cancelación se muestran sin una respuesta inventada | M11/M12 |
| Como usuario controlo mi perfil y privacidad | P1/M | Preferencias sobreviven a nuevo ingreso, sin cruzarse entre cuentas | Bloqueos/silencios/reportes se guardan y solo administración ve reportes | M14 |
| Como revisor resuelvo contenido y apelaciones | P0/L | Decisión exige versión/hash exactos y autor no autoaprueba | Apelación y borrado cumplen responsable y retención acordados | M19/M20 |
| Como equipo publico una versión de prueba | P2/L | HTTPS/DB durable y restauración comprobadas desde dos dispositivos | APK firmado se instala en teléfono y los secretos no están versionados | M16/M17 |
| Como equipo integro Android y web | P2/L | Mismas cuentas/datos/permisos producen resultados equivalentes | Cada entrega enlaza commit, contrato y evidencia verificable | M18/M22/M23/M24 |

Orden de entrega propuesto:
1. Base de identidades, agenda y migraciones; validar datos con Michael.
2. Texto y grupos con revisión humana; revisar flujo con Jery.
3. Resolver almacenamiento/inspección multimodal y estados.
4. Completar tutor real tras saldo, moderación automática y avisos en segundo plano.
5. Integrar web de Pamela, probar dos dispositivos, desplegar y firmar.

No se calcula un porcentaje global. Fórmula a usar después del acuerdo: esfuerzo de historias aceptadas / esfuerzo total acordado. Las pruebas aprobadas por sí solas no sustituyen aceptación funcional del equipo ni del usuario.
