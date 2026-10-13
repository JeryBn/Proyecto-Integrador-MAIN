# Preparación Kotlin — evidencia del 12/10/2026

Responsable: JeryBn, con asistencia de Codex documentada en PROMPTS.md. Tarjeta: https://trello.com/c/ZeMd6ERh.

## Resultado comprobado

- Backend existente: `mvn -B package`, BUILD SUCCESS; 14 pruebas, 0 fallos, 0 errores.
- Android: `gradlew.bat assembleDebug testDebugUnitTest lintDebug`, BUILD SUCCESSFUL. Tres pruebas unitarias de validación de servidor; análisis Android sin errores. Avisos informativos de versiones de dependencias/target SDK permanecen; no se silencian con una baseline.
- Emulador Medium_Phone, Android 17: `gradlew.bat connectedDebugAndroidTest`, tres pruebas instrumentadas aprobadas contra el backend real local.
- `adb reverse tcp:8088 tcp:8088` conecta a Spring Boot sin cambiar su escucha 127.0.0.1.
- APK instalado y pantalla de acceso inspeccionada: [captura](evidencia/01_login.png).

Pruebas instrumentadas: login con sesión mantenida y logout que la invalida; rechazo de contraseña incorrecta sin conservar datos de otra cuenta; consulta sin sesión que produce estado explícito de expiración. Pruebas unitarias: HTTP de desarrollo solo local; HTTPS obligatorio en release; rechazo de rutas, credenciales embebidas y puertos inválidos.

## Pendiente de la tarjeta

La confirmación conjunta de alcance/criterios con equipo y profesor no se ha producido en esta sesión. La preparación técnica cumple los dos ítems de proyecto/conexión y sesión/CSRF; el primer ítem se conserva sin marcar. T07 y T08 se desarrollan en commits separados y no se consideran aceptadas por el equipo a partir de estas pruebas.
