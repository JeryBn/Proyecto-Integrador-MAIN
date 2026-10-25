# Regularización de MAIN al 25 de octubre de 2026

Incremento posterior de la misma fecha: [push Android y moderación automática](PUSH_Y_MODERACION_25_OCT.md). El informe Word/PDF conserva el corte previo y no incorpora ese incremento posterior.

Informe para Jery Becerra Ninaquispe. El corte previsto del sprint 2 era el 20 de octubre. Los cambios del 22 y 25 son posteriores a ese plazo; su integración no acredita entrega puntual ni aceptación del docente. Se preservan autores y fechas de Git, asignaciones históricas de Trello y los informes anteriores.

## Cambios integrados

Pamela Regina Meneses Mayhua aportó los commits originales 9dc5d67, eaea920 y 3adfe0e el 22/10: navegación adaptable y accesibilidad web, revisión de T03/T04, pendientes de T11 y casos sintéticos M20. Se integraron por avance directo de main, sin reescribirlos ni atribuirlos a Jery.

Por encargo de Jery se implementaron el 25/10: f22131c, reconexión web con espera progresiva de hasta 60 segundos, recuperación al volver online y descarte de lecturas de otra cuenta; 2764fe2, carga parametrizable con entorno real declarado; 45469c2, captura web opcional de foto, video con audio y audio; cf1fd88, espera del arranque de MySQL antes de Spring; aa1ee72, ensayo de respaldo y restauración local con comparación de contenidos. Son trabajo asistido registrado con la identidad Git de Jery; no acreditan ejecución personal de sus compañeros.

La captura solo comienza por acción del usuario. Se detienen los dispositivos al cancelar, ocultar la página o desmontar el formulario; límites de 55 segundos y 12 MB. El resultado queda como adjunto local y requiere envío explícito y moderación. Las pruebas controladas no sustituyen la verificación de cámara/micrófono real, que sigue pendiente por interrupción del control de Chrome. No se publicaron capturas personales a contactos.

## Sprint 1

| Tarjetas | Asignación original | Estado verificable y pendiente |
|---|---|---|
| T01 y T02 | Jery Becerra Ninaquispe | API de listado/detalle y autorización implementadas; regresión Spring aprobada. |
| T03 y T04 | Pamela Regina Meneses Mayhua | Web de listado/detalle cerrada técnicamente antes; nueva revisión asistida de Pamela y sus capturas integradas. |
| T05 y T06 | Michael Julian Fernandez Camarena | Cierre técnico delegado previo conservado; nueva regresión no se atribuye como prueba independiente de Michael. |
| T07 y T08 | Jery Becerra Ninaquispe | Cliente Kotlin, vacío, errores, reintento, detalle, rotación y sesión comprobados de nuevo en emulador. |
| T09 | Michael Julian Fernandez Camarena | Operación administrativa implementada y regularizada en Django conforme a la arquitectura; falta acuerdo/aceptación de HU administrativa. |
| T10 | Jery y Michael | MySQL compartido, Flyway y contratos implementados; falta acuerdo humano del propietario de migraciones. |
| T11 | Pamela | React administrativo/Django ya implementados y verificados técnicamente; pendiente acuerdo de alcance y ronda Django/MySQL en su PC. |
| Preparaciones JERY y MICHAEL | Jery y Michael | Código y preparación técnica existentes; conservar pendientes de alcance/aceptación. |
| Preparación PAMELA | Pamela | Evidencia T03/T04 de su PC aportada; no demuestra ZIP administrativo actualizado ni acuerdo T11. |
| HU06 y HU07 | Los tres integrantes | Recorridos implementados y probados; aceptación conjunta pendiente. |
| EQUIPO | Los tres integrantes | Objetivo, capacidad y definición de terminado requieren confirmación humana. |

No se inventa un identificador oficial de HU administrativa ni se firma aceptación en nombre del equipo o docente.

## Sprint 2

En la tabla, los nombres abreviados corresponden a Jery Becerra Ninaquispe, Pamela Regina Meneses Mayhua y Michael Julian Fernandez Camarena. Asignaciones según el backlog histórico, no autoría de cada línea del código.

| Tarjeta | Responsable histórico | Situación al 25 de octubre |
|---|---|---|
| M01 alcance | Equipo | Inventario y criterios disponibles; faltan estimaciones y aceptación acordadas. |
| M02 arquitectura | Jery y Michael | Django administrativo, Spring usuarios y MySQL probados; acuerdo de migraciones pendiente. |
| M03 identidades | Jery y Michael | Cierre técnico existente; 17 comprobaciones HTTP de identidad repetidas. |
| M04 diseño | Jery y Pamela | Aporte adaptable de Pamela integrado; faltan zoom real, lector de pantalla y teléfono. |
| M05 Inicio | Jery | Finalizado técnicamente; conserva cursos, notas y tareas. |
| M06 privados | Jery y Michael | Funcional y persistido; reconexión web mejorada. No se certifican desconexiones prolongadas ni entrega en segundo plano. |
| M07 grupos | Jery y Michael | Cierre técnico existente; invitaciones, roles y revocación conservados. |
| M08 medios | Jery y Michael | Adjuntos implementados; captura web añadida, pendiente comprobación física. |
| M09 estados | Jery y Michael | Texto/medios/audiencias y revisión humana existentes; moderación automática multimodal pendiente. |
| M10 avisos | Jery y Michael | Buzón persistente y consulta implementados; push y entrega con aplicación cerrada pendientes. |
| M11 proveedor IA | Jery y Michael | Asistente definido; evaluación de moderación multimodal y política de datos pendientes. |
| M12 asistente | Jery | Integración existente; último intento real bloqueado por cuota. No hay nueva respuesta real aprobada. |
| M13 administración | Michael y Pamela | Migración Django implementada anteriormente; regresiones administrativas repetidas. |
| M14 perfil | Jery y Pamela | Cierre técnico existente; navegación de perfil comprobada en Android. |
| M15 pruebas | Michael | Nuevas regresiones, HTTP y carga local aprobadas por encargo de Jery; faltan entorno real, red prolongada y aceptación independiente. |
| M16 operación | Michael y Jery | Arranque corregido y restauración local probada; hosting/HTTPS, custodia y operación reales pendientes. |
| M17 entrega Android | Jery | APK debug, 3 unitarias y 8 instrumentadas aprobadas; firma release, teléfono y aceptación pendientes. |
| M18 documentación | Equipo | Informe y evidencias regularizados; cierre final depende de entrega y aceptación. |
| M19 moderación | Jery y Michael | Revisión humana, hash/revisión, privacidad y corrección probadas; clasificación automática multimodal pendiente. |
| M20 normas | Equipo | Categorías iniciales aprobadas y casos de Pamela integrados; revisor, retención y evaluación humana pendientes. |
| M21 web | Pamela | Incremento técnico cerrado anteriormente; ajustes integrados y nueva regresión web aprobada. |
| M22 API compartida | Michael y Jery | Cierre técnico existente; autorización y cruce Django/Spring/MySQL comprobados de nuevo. |
| M23 traspaso | Equipo | Guía y matriz existentes; este informe añade resultados y límites actuales. |
| M24 paridad | Equipo | Cierre del incremento anterior conservado; nueva ronda Android no reemplaza todos los cruces multimedia históricos. |

## Evidencia de la ronda nueva

| Comprobación | Resultado y alcance |
|---|---|
| Spring con Java 21 | 107 pruebas, cero fallos y errores. Primer intento con Java 25 produjo errores de Mockito; no se ocultó ni se contó como aprobado. |
| Django con SQLite de pruebas | 32 pruebas, cero fallos; no representan por sí solas MySQL real. |
| Cliente web | 30 pruebas aprobadas y compilación React. Incluye 4 nuevas de reconexión y 7 de captura controlada. |
| Android local | 3 unitarias; assembleDebug y lintDebug aprobados. Lint conserva 9 advertencias y 4 sugerencias, cero errores. |
| Android instrumentado | 8 pruebas en Medium_Phone, Android 17 y API 37 comprobados con getprop; cero fallos/errores/omitidas. |
| Django y Spring con MySQL real aislado | 17 identidad, 13 académico, 15 horarios/notas y 38 moderación: 83 comprobaciones HTTP; cuentas sintéticas. |
| Carga local | 800 GET autenticados, 8 trabajadores, cero errores; p95 21.12 ms en esta ronda. No es capacidad de producción. |
| Restauración local | 29 tablas y 618 filas comparadas con contenido idéntico; base temporal eliminada; dump privado fuera de Git. |

Las rondas de 107/32/30/3/8 se informan por separado; no se suman como un porcentaje global. No se volvió a ejecutar todo el inventario histórico de multimedia/paridad ni se acreditan teléfonos físicos, IA real, push o producción.

## Bloqueos y aportes necesarios

1. **IA:** Jery habilita saldo/cuota en el proyecto de la clave existente y confirma disponibilidad. No enviar la clave por chat ni crear otra para intentar saltar la cuota.
2. **Firma:** Jery indica ubicación local del keystore y alias, configura contraseñas de forma privada y aprueba la huella del certificado. Después se puede ejecutar PREPARAR-ENTREGA.ps1 y validar APK.
3. **Teléfono:** conectar Android 8 o superior por USB, autorizar depuración e instalación y acordar reemplazo de instalaciones previas. Emulador aprobado no equivale a teléfono probado.
4. **Servidor:** elegir destino y presupuesto; proporcionar acceso seguro al entorno. Verificar HTTPS, base durable y recuperación desde dos dispositivos; la prueba de respaldo local no acredita ese despliegue.
5. **Moderación:** designar revisor de apelaciones y suplente, días de conservación y regla de borrado de pendientes/rechazados/auditoría. Definir aprobación para tratamiento por proveedores externos antes de enviar contenidos reales.
6. **Push:** confirmar proyecto y proveedor de notificaciones, dispositivos objetivo y acceso seguro a configuración; el buzón existente no entrega avisos con la aplicación cerrada.
7. **Aceptación:** equipo/docente confirma HU administrativa, contratos, responsable de migraciones, alcance y resultados por historia. No se sustituyen esos acuerdos con checklists automáticos.
8. **Cámara web:** completar prueba real autorizada cuando Chrome permita el control. La sesión anterior terminó antes de confirmar el permiso y la captura; no se afirma aprobación física.

## Reproducción y fuentes

Usar Java 21. Ejecutar `mvn verify`; en frontend `node --test test/*.test.*` y `node build.mjs`; en admin-django `manage.py test --settings=main_admin.test_settings` con el entorno Python preparado. Android: `testDebugUnitTest assembleDebug lintDebug`; las instrumentadas seleccionaron NavigationUiTest, PendingUiTest y DetailUiTest con puentes ADB a 8088/8090.

La arquitectura local usa MySQL aislado en loopback 3307, Spring 8088 y Django 8090. Credenciales, claves, keystore y dumps permanecen fuera del repositorio. Para restauración, ejecutar verificar_restauracion_local.py solo en la base sintética indicada; requiere binarios MySQL y un usuario local autorizado, nunca una contraseña como argumento público.

Fuentes versionadas: docs/REGULARIZACION_SPRINT_12_OCT.md, docs/PARIDAD_WEB_22_OCT.md, docs/revision/pamela/REVISION_2026_10_22.md y docs/mobile/CIERRE_JERY_SPRINT_2.md. Evidencia nueva en docs/informes/2026-10-25-cierre/. Trello conserva las fechas históricas; las tarjetas del sprint 3 vencen el 27/10 y no modifican el cumplimiento de los sprints anteriores.
