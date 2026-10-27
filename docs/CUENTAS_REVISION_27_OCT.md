# Accesos de revisión de Jery y Michael — 27 octubre 2026

Las dos cuentas están provisionadas y su acceso inicial se comprobó en el entorno local. Sus contraseñas personales siguen pendientes de configuración por cada titular; no se simula ese paso ni la recepción de credenciales por Michael.

| Titular | Usuario | Acceso |
|---|---|---|
| Jery Becerra Ninaquispe | jery.revision | Revisión y apelaciones; responsable |
| Michael Julian Fernandez Camarena | michael.revision | Revisión y apelaciones; suplente |

## Cómo entrar en esta PC

1. Abrir **Accesos de revision MAIN.cmd** en el Escritorio. Seleccionar únicamente el titular correspondiente. La ventana muestra su usuario y mantiene oculta la clave temporal hasta que se solicita verla o copiarla. La copia se limpia al cerrar o después de un minuto si todavía contiene esa clave.
2. Entrar a `http://127.0.0.1:8090/admin.html` con ese usuario y clave temporal.
3. En **Configura tu contraseña**, escribir la temporal como contraseña actual y elegir una nueva personal de al menos doce caracteres. Repetirla y guardar. La temporal no vuelve a funcionar y se revocan las otras sesiones anteriores.
4. Usar **Entrar a moderación y apelaciones**. Cada uno debe cerrar su sesión antes de usar el acceso del otro titular.

La clave temporal de Michael debe entregarse personalmente a Michael por un canal privado. Este incremento no envía claves por WhatsApp, Trello ni GitHub. La dirección indicada funciona en esta PC; para que Michael acceda desde su propio equipo todavía falta el destino HTTPS compartido. No se abrió acceso público a la PC.

## Permisos efectivos

Se creó el rol REVIEWER; no se otorgó ADMIN a las personas. Permite cola y archivos de moderación, decisiones humanas, apelaciones y protección de retención. No permite notas, matrículas, cursos, gestión de cuentas ni auditoría administrativa general. Tampoco sirve como cuenta del portal del alumnado Spring. Los módulos de altas/contactos no pueden otorgar este rol, restablecerlo ni añadirlo como contacto estudiantil; su provisión es una operación de mantenimiento explícita.

Las credenciales se guardan con BCrypt en MySQL. Las temporales de entrega están cifradas con Windows DPAPI en `%LOCALAPPDATA%\MAIN-private\reviewers`, con acceso a la carpeta limitado al usuario Windows actual. No se guardaron claves en los archivos de código, reportes o capturas. El lanzador del Escritorio contiene solamente la ruta del visor, sin credenciales.

Flyway V17 añade password_change_required sin activar el indicador para cuentas existentes. Mientras sea verdadero, el revisor solo puede acceder a su perfil, configurar su contraseña y cerrar sesión; no puede leer contenido pendiente. La sesión Django verifica contraseña y rol actuales: cambiar cualquiera invalida sesiones antiguas. Una denegación a un módulo ajeno devuelve 403 y conserva la sesión de revisión válida. El cambio propio requiere contraseña actual y CSRF, guarda auditoría atómica y limita intentos fallidos. El límite es local al proceso; el despliegue futuro debe usar un contador compartido si se ejecutan varias instancias.

## Verificación

- 78 pruebas Django aprobadas, incluidas limitación de módulos, primer ingreso, cambio propio, CSRF, revocación de otras sesiones, reversión por fallo de auditoría, ausencia de escalación, límite de intentos e independencia al resolver.
- 149 pruebas completas Spring aprobadas, sin errores, fallos ni omitidas. Incluyen dos pruebas nuevas de límites de REVIEWER y seis de apelaciones.
- 37 pruebas web existentes aprobadas y React compilado. En navegador se comprobó el formulario inicial con campos vacíos y la ausencia de enlaces académicos; un revisor sintético ya configurado abrió la cola real. No se introdujo ni cambió una contraseña personal real desde el navegador de pruebas.
- 41 comprobaciones HTTP/MySQL aprobadas. Las dos cuentas reales entraron con sus claves temporales recuperadas en memoria de DPAPI; ambas conservaron el requisito de cambio personal y se rechazó su acceso a módulos ajenos. El circuito de rechazo, apelación, resolución independiente y aprobación posterior se probó con revisores sintéticos de este mismo rol.
- Los tres revisores sintéticos se deshabilitaron después de la comprobación visual. La provisión se ejecutó de nuevo y confirmó que las cuentas existentes no se sobrescriben ni se les cambia la clave.
- El visor de claves cargó sus componentes Windows en modo ValidateOnly sin mostrar ni descifrar secretos. La recuperación DPAPI se comprobó con las solicitudes de acceso inicial, sin imprimir claves.

Evidencia sin secretos: docs/informes/2026-10-27-cuentas. scripts/verificar_cuentas_revision.py recibe las claves reales iniciales solo por entorno temporal y nunca las modifica. La prueba de cambio personal utiliza exclusivamente cuentas sintéticas. scripts/Provision-ReviewAccounts.ps1 es explícito, rechaza colisiones y no cambia cuentas existentes; no se añade al arranque automático. scripts/Show-ReviewAccess.ps1 se abre manualmente por el titular.

## Pendientes que conservan su estado real

Cada titular debe configurar su contraseña y confirmar su propio ingreso. Provisionar y comprobar técnicamente sus cuentas no acredita ese acto personal ni la lectura de Michael. La evaluación de moderación automática sigue bloqueada por cuota/proveedor y no se activó al crear los accesos. Permanecen también los bloqueos previos de FCM, HTTPS y teléfono físico. No se declaran M19/M20 terminadas globalmente por este incremento.
