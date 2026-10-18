# MAIN Android — M04 y M05

**Revisión posterior:** consultar [estado actual y pendientes](../ESTADO_IMPLEMENTACION_2026_10_18.md). Los resultados siguientes son evidencia histórica anterior a la integración de agenda, mensajes y asistente.

## Implementado en esta revisión

- Se añadió una identidad visual Material 3 con paleta MAIN, tipografía, formas y tema claro/oscuro.
- El tema oscuro se puede activar desde Perfil; el ajuste vive en memoria durante la sesión.
- Se añadió navegación Android para Inicio, Actividades, Mensajes, Estados, Asistente y Perfil.
- Inicio muestra el perfil y las actividades que devuelve la API autenticada; no incorpora horarios ni información académica ficticia.
- Mensajes, Estados y Asistente indican que sus servicios siguen en preparación. No presentan conversaciones, publicaciones ni respuestas simuladas.
- Los formularios, lista y detalle existentes reciben el tema común sin cambiar la autorización ni el contrato de T07/T08.
- Cerrar sesión vuelve a Inicio de sesión y limpia el estado de navegación y los datos de la cuenta.

## Archivos

- `mobile/app/src/main/java/pe/tecsup/main/mobile/MainActivity.kt`: tema y entrada de la aplicación.
- `mobile/app/src/main/java/pe/tecsup/main/mobile/MainNavigationScreen.kt`: destinos, navegación, Inicio, Perfil y estados de módulos no conectados.
- `mobile/app/src/main/java/pe/tecsup/main/mobile/MainViewModel.kt`: destino seleccionado y apariencia de la sesión.
- `mobile/app/src/androidTest/java/pe/tecsup/main/mobile/NavigationUiTest.kt`: recorrido de destinos, perfil y apariencia.

## Alcance que requiere coordinación

La agenda y el horario requieren historias y datos autorizados acordados en M01/M02. Mensajes, estados, asistente, medios, notificaciones y controles de privacidad requieren contratos y servicios compartidos (M06–M12, M14, M19, M22). Las vistas de esta revisión identifican esas áreas sin anunciar funciones inexistentes como listas.

La aceptación del equipo/profesor, la demostración en teléfono físico y la firma de una versión de entrega son verificaciones externas; no se atribuyen a esta implementación.

## Verificación

- Android: `testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest` — `BUILD SUCCESSFUL`. Pasaron 3 pruebas unitarias y 15 instrumentadas contra el servidor real, sin fallos, errores ni casos omitidos. Lint terminó con 0 errores y 5 avisos sobre versiones de dependencias/SDK.
- APK: `mobile/app/build/outputs/apk/debug/app-debug.apk`.
- Backend: `mvn verify` — 29 pruebas aprobadas, sin fallos, errores ni casos omitidos; JAR generado. La repetición `mvn -o test` también pasó con los permisos normales del entorno.
- Se corrigió el arranque de Mockito en Java 21 usando un agente explícito de Surefire, sin conexión dinámica.
- Instrumentación Android: ejecutada en `Medium_Phone`, Android 17/API 37, con `adb reverse` hacia `127.0.0.1:8088`. El servidor de la pasada final usó JDK 21 y H2 demo.
- La navegación comprobó las seis secciones, el cambio de tema, su conservación al recrear la pantalla y el cambio de cuenta sin conservar los datos de la anterior. Las pruebas existentes comprobaron autenticación, autorización, listado, detalle, búsqueda, filtros, vacío, errores, reintento, sesión vencida y cierre de sesión.
- Se revisaron capturas de Inicio y Perfil oscuro; se corrigieron etiquetas inferiores recortadas y el contraste de los iconos del sistema.
- Procedimiento reproducible y rutas de reportes: [Verificación local](VERIFICACION_LOCAL.md).

M04/M05 quedan implementados y probados en el emulador para revisión del equipo. La aceptación compartida, la prueba física y la revisión de accesibilidad y tamaños adicionales siguen pendientes antes de cerrar las tarjetas.
