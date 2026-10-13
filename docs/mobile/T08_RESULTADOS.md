# T08 — Detalle Kotlin y regreso al listado

Fecha: 12/10/2026. Responsable: JeryBn con asistencia de Codex. Tarjeta: https://trello.com/c/m1xaBVrG.

## Cambio entregado

Desde cada actividad del listado se abre el detalle autorizado. Se muestran título, indicaciones, curso, docente, fecha límite y estado del servidor. El botón de regreso y Atrás de Android mantienen búsqueda y filtro. El ViewModel conserva sesión y pantalla al recrear Activity; respuestas tardías se descartan si ya se regresó o cambió de cuenta.

404 muestra un mensaje de actividad no disponible sin revelar información ajena. 401 elimina perfil, actividades, detalle y filtros, vuelve al acceso y permite ingresar con otra cuenta. Login y apertura de detalle cierran el teclado para dejar visible el contenido.

## Verificación final de la APK

`gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest`: BUILD SUCCESSFUL, tres pruebas unitarias y catorce instrumentadas, cero fallos. Lint no presenta errores; conserva seis advertencias informativas de versiones/compatibilidad y reglas de copia. No se omiten comprobaciones ni se añade baseline para ocultarlas.

Las cinco pruebas añadidas para T08 comprueban:

1. Los campos completos del detalle autorizado desde el backend real.
2. Actividad ajena/inexistente devuelve 404; detalle sin sesión devuelve 401.
3. Navegación listado → detalle → recreación de Activity → regreso con búsqueda/filtro; también Atrás de Android.
4. Mensaje de 404 en la app, ausencia de título privado y regreso al listado.
5. Expiración durante el detalle, borrado de datos anteriores e ingreso posterior como vacio.

La quinta prueba usa el puente de tests para omitir la cookie en una petición: el backend real responde 401. Las respuestas y datos del resto del recorrido provienen de Spring Boot. El puente y las capturas están solo en androidTest, no en la APK entregada.

## Evidencia inspeccionada

- [Detalle autorizado](evidencia/07_detalle_autorizado.png).
- [Regreso con contexto](evidencia/08_regreso_con_contexto.png).
- [Actividad no disponible](evidencia/09_detalle_no_disponible.png).
- [Sesión finalizada](evidencia/10_sesion_finalizada.png).

Se actualizaron las capturas del listado para mostrar la versión final y se añadieron instalación por USB, arquitectura móvil y compilación Android en GitHub. La ejecución de GitHub se comprobará tras publicar este commit; no se atribuye éxito remoto a la verificación local.

## Límites

Validación en emulador Medium_Phone con Android 17; teléfono físico pendiente. Se entrega una APK debug para demostración, con backend local y datos sintéticos. No se implementa Django, migraciones compartidas, usuarios persistidos ni IA. La revisión independiente de Michael y la aceptación de las HUs por el equipo permanecen pendientes.
