# MAIN Android — parte de Jery

Revisión de interfaz y notas: [Chats, privados y calificaciones por curso](../docs/CHATS_Y_NOTAS_2026_10_18.md). Inicio mantiene el centro académico. Chats → Nuevo chat permite iniciar privados con estudiantes o profesores; la privacidad se gestiona desde la información de cada contacto.

Cliente Kotlin y Jetpack Compose para actividades, agenda autorizada, perfiles, mensajes de texto revisados, grupos, avisos y asistente académico. Consume Spring Boot. Estados y adjuntos multimedia siguen pendientes.

Estado actual: [implementación y pendientes](../docs/ESTADO_IMPLEMENTACION_2026_10_18.md), [IA y despliegue](../docs/IA_Y_DESPLIEGUE.md). Las evidencias anteriores de HU06/HU07 se conservan como historial.

## Alcance y aceptación

- Preparación Kotlin: proyecto reproducible, conexión documentada, sesión real y CSRF. Revisión de alcance por equipo/profesor pendiente; implementar por solicitud de Jery no implica aprobación de terceros.
- T07: listado de pendientes con título, curso, fecha y estado recibidos del servidor; carga, reintento y vacío; comprobar alumno y vacio.
- T08: detalle autorizado, campos completos, 404 y sesión finalizada; volver preserva contexto. Incluye botón Atrás de Android y conservación de sesión/contexto al recrear la pantalla.
- Dependencias: backend T01/T02. Cada tarea se valida antes de registrar su commit y checklist.

## Entorno

Android Studio, JDK 21, SDK Android 36, Gradle 8.13 (wrapper incluido), Android 8 o superior. Abrir esta carpeta como proyecto independiente; el backend Maven sigue en la raíz del repositorio.

## Conexión local sin exponer el backend

1. En la raíz: `mvn package` y `java -jar target/main-sprint1-0.1.0.jar`. El servidor escucha en 127.0.0.1:8088.
2. Iniciar emulador o conectar dispositivo USB con depuración autorizada.
3. Ejecutar `adb reverse tcp:8088 tcp:8088` para el dispositivo seleccionado.
4. Compilar: `gradlew.bat assembleDebug testDebugUnitTest lintDebug`.
5. Instalar `app/build/outputs/apk/debug/app-debug.apk`. Servidor: `http://127.0.0.1:8088`.
6. Demo: cuentas alumno, otro y vacio, contraseña sintética `MainDemo2026!`.
7. Con backend iniciado y puente USB: `gradlew.bat connectedDebugAndroidTest`.

Opcional: en PowerShell `& .\INSTALAR-APP.ps1` instala el APK compilado y configura el puente para un único dispositivo. Si hay varios, indicar `-Device emulator-5554` o el identificador correspondiente. El script no inicia ni modifica otros servidores.

En Android Studio seleccionar Gradle JDK 21. Si se ejecuta desde una terminal, configurar JAVA_HOME hacia el JDK 21 instalado antes de usar el wrapper. No subir local.properties: cada instalación de Android Studio genera su ruta al SDK.

La variante debug admite HTTP únicamente en localhost, 127.0.0.1 y 10.0.2.2. La variante release exige HTTPS. Para servidor real indicar un origen HTTPS sin rutas ni credenciales. No se abre el servidor a la red ni se desactiva la verificación TLS.

## Contrato

GET /login-token devuelve parameterName y token. POST /login usa formulario y redirección; el cliente conserva la cookie y valida GET /api/perfil. GET /api/actividades/pendientes conserva orden y estado del servidor. GET /api/actividades/{id} autoriza en backend. POST /logout usa token fresco de GET /api/csrf. 401 elimina la sesión local y pide volver a ingresar; 404 no revela datos ajenos.

No se persisten contraseña ni cookies. Rotación conserva la sesión en ViewModel; tras terminar el proceso se requiere ingresar de nuevo. Fecha del backend sin zona se interpreta America/Lima, sin recalcular vencimiento. La conexión por USB requiere volver a establecer adb reverse después de reiniciar el dispositivo.

## Ruta de trabajo

Preparación y acceso → T07 listado → T08 detalle → validación y entrega. El esquema compartido y Django permanecen a cargo del trabajo conjunto T10. La app reutiliza el contrato del backend disponible.

El flujo Android de GitHub compila, ejecuta pruebas unitarias y analiza lint; adjunta una APK debug por ejecución. Las pruebas instrumentadas contra Spring Boot se ejecutan localmente y se documentan en docs/mobile/. No se afirma que el flujo de CI haya pasado hasta comprobar su ejecución.

El procedimiento de arranque del emulador, conexión al servidor y ejecución completa está en [Verificación local](../docs/mobile/VERIFICACION_LOCAL.md). Los cambios y resultados de la navegación Material 3 se registran en [M04/M05](../docs/mobile/M04_M05_RESULTADOS.md).
