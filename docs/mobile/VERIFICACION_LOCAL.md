# MAIN — verificación local de servidor y Android

**Revisión posterior:** ver [estado actual](../ESTADO_IMPLEMENTACION_2026_10_18.md) y [IA/avisos/despliegue](../IA_Y_DESPLIEGUE.md). Los recuentos siguientes corresponden a la ejecución anterior. Los nuevos contratos Android requieren un servidor demo aislado con administrador sintético explícito: main.admin.username=android_test_admin y main.admin.password=SyntheticAndroidTest2026!; no usar contra datos reales. AssistantUiTest hace una consulta sintética al proveedor; un error explícito no se considera evidencia de generación.

## Resultado del 18 de octubre de 2026

| Verificación | Resultado |
|---|---|
| Maven `verify`, JDK 21 | 29 pruebas aprobadas; JAR generado |
| Repetición Maven `-o test` con permisos normales | 29 aprobadas; confirma que ya no se necesita conexión dinámica de Mockito |
| Android unitarias | 3 aprobadas |
| Android instrumentadas contra Spring Boot | 15 aprobadas en Medium_Phone, Android 17/API 37 |
| Android `assembleDebug` | APK generado |
| Android `lintDebug` | 0 errores, 5 avisos de versiones de dependencias/SDK |
| `git diff --check` | Sin errores |

Son 47 casos distintos aprobados, sin fallos, errores ni casos omitidos. Las pruebas del servidor se repitieron para comprobar la corrección del entorno; no se cuentan dos veces en ese total. La pasada Android final terminó con `BUILD SUCCESSFUL` en 2 minutos 13 segundos. El servidor que consumió la app se ejecutó con JDK 21, H2 demo y escucha local en `127.0.0.1:8088`.

Cobertura comprobada: autenticación y cierre de sesión, CSRF, autorización y separación de alumnos, listado y detalle, estados vacíos, búsqueda y filtros, errores y reintento, sesión vencida, rotación/regreso con contexto, navegación, tema y cambio de cuenta. También pasaron las pruebas existentes de credenciales administrativas y creación/edición/autorización de actividades.

## Correcciones del entorno y las pruebas

El emulador no podía crear sus archivos de bloqueo en la carpeta `.android` del usuario desde el entorno restringido de ejecución. Se inició el AVD existente con acceso permitido a esa carpeta, sin borrar sus datos ni cargar/guardar snapshots. Android terminó de arrancar y ADB lo identificó como `emulator-5554`.

Las pruebas del servidor quedaban detenidas durante la conexión dinámica del agente Mockito. `pom.xml` ahora obtiene la ruta de Mockito desde las dependencias de Maven y la pasa a Surefire mediante `-javaagent` al iniciar la JVM de pruebas. La versión sigue siendo la administrada por Spring Boot. Se aplicó la configuración descrita en la [documentación de Mockito para Java 21](https://www.javadoc.io/static/org.mockito/mockito-core/5.21.0/org.mockito/org/mockito/Mockito.html#0.3).

Las pruebas de interfaz se ajustaron al flujo Inicio → Actividades y a listas que componen elementos al desplazarse. El caso de servidor inaccesible espera primero el error y comprueba la recuperación después de corregir la dirección. Los casos de detalle y mensajes vacíos/de error desplazan la lista hasta el elemento que se comprueba.

La inspección de capturas detectó etiquetas inferiores recortadas y escaso contraste de los iconos del sistema en tema oscuro. Se usan las etiquetas compactas Tareas e IA, conservando Actividades y Asistente como descripciones accesibles, y las barras del sistema se actualizan al cambiar de tema.

## Repetir la verificación

Usar JDK 21 en `JAVA_HOME`. Configurar `ANDROID_HOME` con el SDK instalado. En esta PC el SDK está en `%LOCALAPPDATA%\Android\Sdk`; el AVD instalado se llama `Medium_Phone`.

En la raíz del repositorio:

```powershell
mvn --batch-mode --no-transfer-progress verify
& (Join-Path $env:JAVA_HOME 'bin\java.exe') -jar target/main-sprint1-0.1.0.jar
```

El segundo comando mantiene el servidor local en ejecución en `127.0.0.1:8088`. Si ya existe otro servidor en ese puerto, confirmar su origen antes de ejecutar las pruebas.

En otra terminal, iniciar el emulador:

```powershell
& (Join-Path $env:ANDROID_HOME 'emulator\emulator.exe') -avd Medium_Phone -no-snapshot-load -no-snapshot-save
```

Cuando termine de arrancar, en otra terminal:

```powershell
$adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
& $adb devices -l
& $adb -s emulator-5554 shell getprop sys.boot_completed
& $adb -s emulator-5554 reverse tcp:8088 tcp:8088
```

El estado debe ser `device` y `sys.boot_completed` debe devolver `1`. Si el identificador es distinto, sustituirlo también en `ANDROID_SERIAL`:

```powershell
$env:ANDROID_SERIAL = 'emulator-5554'
Set-Location mobile
.\gradlew.bat --no-daemon --console=plain --max-workers=2 '-Pkotlin.compiler.execution.strategy=in-process' '-Pkotlin.incremental=false' testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest
```

Los contratos instrumentados usan las cuentas sintéticas del servidor demo. No ejecutarlos contra un servidor productivo ni contra una base con datos personales. Las pruebas de interfaz abren la app y prueban cierre/cambio de sesión; las pruebas administrativas del servidor usan su propia base H2 aislada.

## Reportes generados

- Servidor: `target/surefire-reports/`.
- Android unitarias: `mobile/app/build/test-results/testDebugUnitTest/`.
- Android instrumentadas: `mobile/app/build/outputs/androidTest-results/connected/debug/` y `mobile/app/build/reports/androidTests/connected/debug/`.
- Lint: `mobile/app/build/reports/lint-results-debug.html`.
- APK debug: `mobile/app/build/outputs/apk/debug/app-debug.apk`.
- Capturas instrumentadas: `Pictures/MAIN-evidence` dentro del emulador.

El resultado automatizado cubre los contratos y recorridos implementados. No constituye aprobación del equipo/profesor, prueba de teléfono físico, firma de entrega, validación de módulos sin servicio ni paridad con Web.
