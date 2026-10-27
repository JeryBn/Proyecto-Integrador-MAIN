# Avances y pendientes de MAIN al 27 de octubre de 2026

Informe para Jery Becerra Ninaquispe. Se revisaron las 51 tarjetas del tablero MAIN: 19 estaban en FINALIZADO y 32 abiertas. De las abiertas, 23 conservan vencimientos del 12 o 20 de octubre y nueve vencen hoy 27 de octubre a las 23:59, America/Bogota. Estar vencida no demuestra ausencia de código: varias tarjetas conservan aceptación, infraestructura o pruebas físicas pendientes. No se calcula un porcentaje global por número de tarjetas ni se cambia el plazo histórico.

La revisión del 27 añade dos incrementos publicados en main: comprobación de las seis parejas de roles en conversaciones privadas y recuperación de consultas web atascadas. La regresión completa del servidor aprobó 133 ejecuciones; el cliente web aprobó 34 pruebas y compiló. Todavía no corresponde declarar todo el proyecto terminado.

Después de documentar la revisión se cerró S3-09, seguimiento de este corte, con sus tres criterios comprobados y sin alterar asignaciones históricas. El tablero pasa a 20 tarjetas en FINALIZADO y 31 abiertas: 23 vencidas y ocho con vencimiento el 27. Este cierre no completa las funcionalidades pendientes de las otras tarjetas.

## Trabajo elaborado en esta revisión

**Conversaciones entre roles.** Commit 4d42dfb. Se añadieron seis casos parametrizados que cubren alumno con alumno, alumno con docente, alumno con familiar, docente con docente, docente con familiar y familiar con familiar. Cada caso comprueba ambos sentidos del envío. Sin contacto autorizado no se inicia la conversación; invertir los participantes conserva el mismo privado; el pendiente no se muestra al receptor; solo la aprobación permite verlo. También se comprueban reintento sin duplicación, CSRF, acceso de una tercera cuenta, ocultación de huella y motivo privados, y bloqueo de nuevos envíos. Estas son pruebas de integración Spring con H2, no seis ensayos en teléfonos reales ni una aceptación de la matriz por el docente.

**Recuperación web ante una lectura atascada.** Commit f724c58. Las consultas GET ahora tienen un plazo de 30 segundos y una señal de cancelación propia. Antes, una conexión que no terminara podía dejar ocupada la actualización periódica indefinidamente. El vencimiento libera la consulta para que el mecanismo de reconexión existente vuelva a intentarlo con espera progresiva. Una lectura posterior obtiene una señal nueva; desmontar la pantalla cancela la anterior y limpia su temporizador. No se reenvían automáticamente escrituras ni adjuntos con resultado incierto.

**Pruebas de desconexión.** Tres pruebas nuevas comprueban vencimiento, recuperación y limpieza de cancelaciones. Una cuarta simula dos horas de fallos del servidor con reloj controlado: mantiene un solo temporizador, limita la frecuencia y vuelve al intervalo normal cuando se recupera. No se afirma haber pasado dos horas en una red móvil real. El cliente compilado del backend quedó actualizado en el mismo commit.

**Auditoría de Trello.** Se leyeron las 32 tarjetas abiertas, conservando responsables, plazos y antecedentes. Las instrucciones posteriores de Jery autorizan continuar también módulos asignados a otros integrantes; los límites de colaboración históricos de las tarjetas no impiden este encargo actual. No se marca aceptación personal de Pamela, Michael ni del profesor.

## Resultados y alcance de las pruebas

| Ronda del 27 de octubre | Resultado |
|---|---|
| Matriz privada de roles aislada | 6 casos aprobados sin fallos, errores ni omitidos. |
| Regresión Spring completa con Java 21 | 133 ejecuciones aprobadas; incluye la matriz y diez ejecuciones del caso de retry push. No sumar de nuevo los seis casos aislados. |
| Cliente web | 34 pruebas aprobadas; cero fallos u omitidas. Compilación React aprobada. |
| Teléfono por ADB | Ningún dispositivo conectado en la inspección de esta ronda. No hubo pruebas físicas nuevas. |
| Android, Django y proveedor real | No se volvieron a ejecutar en esta ronda; los resultados del 25 son evidencia histórica separada. |

La ronda anterior del 25 acredita 43 pruebas Django, cuatro unitarias Android y dos instrumentadas de Firebase/vinculación, además de dos consultas sintéticas reales de moderación y ocho comprobaciones MySQL/Django/Spring con proveedor controlado. Obtener un token de Firebase no acredita recibir un aviso. Las 133 ejecuciones de Spring incluyen nueve repeticiones adicionales de un caso de retry; no representan 133 casos diferentes.

## Responsables y autoría

Los responsables históricos son Jery Becerra Ninaquispe, Pamela Regina Meneses Mayhua y Michael Julian Fernandez Camarena. Las asignaciones de Trello describen responsabilidad de coordinación, no permiten atribuir cada línea ni afirmar que alguien no trabajó. Los commits originales de Pamela 9dc5d67, eaea920 y 3adfe0e permanecen integrados con su autoría. La implementación administrativa original de Michael b386291 se conserva como antecedente de la migración a Django solicitada por Jery.

Los incrementos de este informe son trabajo asistido por encargo de Jery, registrado en su main. No se presentan como pruebas personales e independientes de sus compañeros. Las integraciones y regularizaciones posteriores al 20 no acreditan una entrega puntual del sprint 2.

## Tarjetas abiertas de los sprints anteriores

En la columna de responsables, Equipo significa los tres integrantes; Jery y Michael, o Jery y Pamela, conservan sus nombres completos indicados arriba. El siguiente paso es el requisito que impide el cierre global; los entregables técnicos ya existentes no deben rehacerse.

| Tarjeta y responsable histórico | Qué está listo | Qué falta para cerrar |
|---|---|---|
| EQUIPO — Equipo | Objetivo y definición de terminado propuestos; implementación disponible. | Acuerdo real de alcance, capacidad y aceptación. |
| HU06 — Equipo | Listado web y Kotlin, búsqueda, filtros, permisos y vacío. | Aceptación conjunta y plataformas comprometidas. |
| HU07 — Equipo | Detalle autorizado, campos, navegación y errores. | Aceptación conjunta del recorrido y alcance móvil. |
| Preparación JERY — Jery | Kotlin HU06/HU07 implementado, sesión y emulador comprobados. | Acuerdo histórico de alcance; teléfono se verifica en M17. |
| T09 — Michael | Operación administrativa y regularización Django integradas y probadas. | HU administrativa y cambio arquitectónico aceptados formalmente. |
| Preparación MICHAEL — Michael | Backend original conservado; integración y persistencia resueltas. | Acuerdo de HU, operación y migraciones; revisión propia si se exige. |
| T11 — Pamela | React administrativo conectado a Django; permisos y persistencia probados. | Acuerdo de HU y ronda propia en su PC. |
| Preparación PAMELA — Pamela | Revisión T03/T04 aportada por Pamela e integrada. | Paquete administrativo actualizado y acuerdo T11 en su entorno. |
| T10 — Jery y Michael | MySQL compartido; Flyway dueño del esquema y Django sin DDL de negocio. | Acordar custodio de migraciones y entornos. |
| M02 — Jery y Michael | Arquitectura Django/Spring, contratos y persistencia comprobados. | Acuerdo operativo y entorno compartido definitivo. |
| M01 — Equipo | Inventario y criterios disponibles. | Prioridades, estimaciones aceptadas y alcance de entrega. |
| M04 — Jery y Pamela | Material 3, temas y navegación; mejora adaptable de Pamela integrada. | Accesibilidad con lector, zoom real y teléfono. |
| M06 — Jery y Michael | Privados, moderación, paginación y recuperación web; seis parejas comprobadas hoy. | Red móvil prolongada y aceptación; entrega cerrada depende de M10. |
| M09 — Jery y Michael | Estados, audiencia, caducidad y revisión humana. | Evaluación multimodal automática y política de retención M19/M20. |
| M10 — Jery y Michael | Buzón, registro cifrado FCM, cola y token Firebase real. | Credencial FCM servidor, recepción real, destinos y recordatorios configurables. |
| M11 — Jery y Michael | Asistente definido y moderación textual/imagen probada con contenido sintético. | Cobertura audio/video, evaluación y tratamiento de datos aprobado. |
| M12 — Jery | Asistente integrado; recuperación y límites controlados. | Respuesta académica real con cuota disponible, Android y web. |
| M15 — Michael | Regresiones, permisos, carga y restauración locales; nueva ronda del 27 aprobada. | Red y dispositivo reales, entorno final y aceptación independiente requerida. |
| M16 — Michael y Jery | Arranque local y respaldo/restauración ensayados. | Destino HTTPS, acceso, presupuesto y operación desde dos dispositivos. |
| M17 — Jery | APK debug y emulador con pruebas históricas aprobadas. | Firma release, URL final, teléfono y aceptación por historia. |
| M18 — Equipo | Manuales, contratos, evidencias e informes versionados. | Paquete final reproducible y aceptación tras resolver M17 y bloqueos. |
| M19 — Jery y Michael | Moderación humana y trabajador automático textual con control de versión y reintentos. | Audio/video y evaluación, política y operación aprobadas. |
| M20 — Equipo | Categorías iniciales aprobadas y casos de Pamela integrados. | Revisor, suplente, retención, borrado y evaluación humana de errores. |

No se ha certificado la ausencia de errores en funciones que no se volvieron a probar. El comportamiento ante audio/video sigue requiriendo implementación y evaluación: no es únicamente un archivo de configuración pendiente. Las imágenes se derivan a revisión humana por cobertura parcial.

## Sprint 3 que vence el 27 de octubre

| Tarjeta | Preparación disponible | Acción pendiente |
|---|---|---|
| S3-01 Alcance | Revisión de main e inventario de 32 abiertas. | Registrar historias de entrega y acuerdos reales del equipo. |
| S3-02 IA | Integración y pruebas controladas existentes. | Cuota y consulta real en ambos clientes; no efectuar pagos desde la asistencia. |
| S3-03 Normas | Categorías y puerta de política implementadas. | Jery designa revisor/suplente y plazos con excepciones. |
| S3-04 HTTPS | Configuración local y requisitos documentados. | Elegir destino, presupuesto y custodio; proporcionar acceso seguro. |
| S3-05 Firma | Configuración por entorno y verificador de entrega disponibles. | Keystore/alias, contraseñas privadas, huella aprobada y URL definitiva. |
| S3-06 Teléfono | Casos y APK debug disponibles. | Conectar Android por USB y autorizar depuración; ejecutar y guardar evidencia. |
| S3-07 Integración | Android usa API compartida y sesión con datos locales. | Consumir HTTPS final y verificar contrato en ese entorno. |
| S3-08 Informe | Este informe actualiza avances y límites a la ronda del 27. | Aceptación humana, APK release y demostración final con evidencia. |
| S3-09 Coordinación | Seguimiento del corte completado; tres criterios y estado FINALIZADO verificados en Trello. | Sin pendiente en este corte; las funciones de las tarjetas consultadas siguen su propio estado. |

Las nueve conservan la fecha indicada por Jery; S3-09 está cumplida y las otras ocho siguen abiertas. Se distingue preparación comprobada de criterios que todavía no están satisfechos.

## Información concreta necesaria para terminar

**Push.** El proyecto main-integrador-jery-2026 y su app Android ya existen. Falta un archivo JSON de cuenta de servicio autorizado para FCM HTTP v1, guardado fuera del repositorio. Jery debe proporcionar solo su ruta privada local; no enviar su contenido por chat ni Trello. Después se puede configurar el servidor y comprobar envío y recepción. No se ha creado esa credencial ni ampliado permisos de la cuenta en esta ronda.

**Moderación.** Se necesitan nombre de usuario ADMIN del revisor, suplente, días de conservación separados para pendientes/rechazados/auditoría, excepciones y aprobación del tratamiento externo. No se ejecutará una purga con plazos inventados. La evaluación de texto e imagen del 25 no completa audio/video ni demuestra una precisión absoluta.

**Entrega.** Elegir proveedor o servidor y URL HTTPS, presupuesto autorizado y acceso seguro. Proporcionar ubicación del keystore y alias; configurar sus contraseñas de forma privada y aprobar la huella del certificado. Conectar un teléfono para probar permisos, medios y notificaciones. Ningún teléfono estaba conectado en la inspección del 27.

**Aceptación académica.** Registrar decisión de alcance, HU administrativa, custodio de migraciones y aceptación por historia. El asistente puede elaborar matrices y corregir código; no firmar aceptación de Pamela, Michael o el docente. La cuota del asistente académico debe comprobarse con una respuesta real: que moderación haya respondido no acredita acceso al modelo del asistente.

## Orden de continuación

Primero resolver acceso FCM y política de moderación, para desbloquear pruebas de notificación y evaluación. En paralelo, decidir HTTPS/firma y conectar teléfono. Después ejecutar la matriz de recorridos entre Android y web contra ese entorno, corregir defectos, preparar APK firmado y registrar aceptación. Audio/video y recordatorios siguen siendo desarrollo técnico pendiente que puede avanzarse con esos contratos y decisiones; no se dan por terminados en este informe.

Cada incremento comprobado se publica por separado en main y se vincula en Trello. Se conservan las fechas históricas y la autoría. Las tarjetas completas solo deben marcarse cuando todos sus criterios estén verificados; las subtareas comprobadas pueden cerrarse sin presentar una tarjeta global bloqueada como completa.

## Evidencia reproducible

Repositorio: https://github.com/JeryBn/Proyecto-Integrador-MAIN

Tablero: https://trello.com/b/vrcqu5di/2026-2-c24-a-sprint-1

Ronda nueva: mvn test con Java 21; node --test test/*.test.* y node build.mjs en frontend. Fuente de matriz: DirectRoleMatrixTest.java. Recuperación: bounded-read.mjs, user-api.mjs y sus pruebas. Inventario y resultados de esta ronda en docs/informes/2026-10-27-revision/. Configuración y pruebas anteriores detalladas en docs/PUSH_Y_MODERACION_25_OCT.md; no se incluyen secretos ni tokens en el informe.
