# T07 — Listado Kotlin de actividades pendientes

Fecha: 12/10/2026. Responsable: JeryBn con asistencia de Codex. Tarjeta: https://trello.com/c/rpLwU0IQ.

## Cambio

La app inicia sesión contra Spring Boot, consulta pendientes y muestra título, curso, fecha y estado recibido del servidor. Incluye carga, actualización, reintento, estado vacío, búsqueda por título/curso y filtros Todas/Vencidas/Próximas. Mantiene el orden del servidor y no recalcula vencimiento ni matrícula. Logout y expiración eliminan datos de la cuenta anterior.

## Verificación

`gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest`: BUILD SUCCESSFUL. Tres pruebas unitarias y nueve instrumentadas, cero fallos. Lint: cero errores, seis avisos informativos de versiones/compatibilidad y copia de datos. No se añadieron excepciones para esconder errores.

- Cinco pruebas de contrato real: sesión/login/logout, rechazo de credenciales, 401 sin sesión, listado del alumno y aislamiento de vacio/otro.
- Cuatro pruebas de interfaz: listado y búsqueda, cuenta vacía, fallo de conexión con recuperación, fallo de consulta con Reintentar que recupera los datos.
- Para comprobar el último caso se usa únicamente en tests ApiBridge, un puente local hacia el backend real que inyecta una respuesta 503 y luego permite la consulta normal. No modifica el servidor ni añade código de simulación a la APK.
- Se fijó Espresso 3.7.0, cuya corrección de InputManager es necesaria para las pruebas del emulador Android 17: https://developer.android.com/jetpack/androidx/releases/test.

## Evidencia visual inspeccionada

- [Listado alumno](evidencia/02_listado_alumno.png).
- [Sin coincidencias](evidencia/03_sin_coincidencias.png).
- [Cuenta vacía](evidencia/04_cuenta_vacia.png).
- [Error de conexión](evidencia/05_error_conexion.png).
- [Error de consulta y reintento](evidencia/06_error_listado.png).

Las actividades sintéticas tienen fechas anteriores a la revisión; sus estados vencidos provienen del backend y no se alteran para la captura. La ejecución acredita T07 en emulador, no una prueba en teléfono físico ni la aceptación independiente de Michael. T08 incorpora la apertura de detalle en su siguiente commit.
