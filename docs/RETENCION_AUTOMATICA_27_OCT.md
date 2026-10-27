# Retención automática de moderación

Política delegada por Jery, versión jery-2026-10-27-v1: pendientes siete días, rechazados treinta días, historial de moderación noventa días. La implementación no depende de OpenAI, Firebase ni HTTPS.

Flyway V15 registra la presentación de cada revisión y la fecha de minimización. Los datos heredados reciben la fecha de migración como inicio conservador, incluso si el mensaje original es antiguo; no se elimina inmediatamente contenido por una edad que no se conocía. Los rechazados cuentan desde la decisión o inicio conservador, lo que sea posterior.

La limpieza vacía texto/motivo y elimina bytes de archivos sin referencias vigentes. Conserva una marca mínima de eliminación con ID, hash y fecha para integridad e idempotencia. No borra mensajes publicados ni un archivo compartido por otra revisión conservada. Los estados pendientes/rechazados eliminados desaparecen del feed. No se eliminan automáticamente estados aprobados, conversaciones, identidades, notas o actividades. La auditoría ajena a moderación permanece fuera de alcance. Las copias de seguridad existentes requieren su propia política: esta limpieza no las reescribe.

Desde Revisión de mensajes, un ADMIN autenticado puede proteger contenido por apelación y resolver esa protección con motivo obligatorio. La lista de protecciones incluye contenido que ya salió de la cola. También puede introducir el identificador de un rechazado. La protección mantiene todo el contenido del asunto y su auditoría; al resolverla empieza un plazo completo nuevo. Una protección no recupera contenido ya eliminado. No se implementa con ello una resolución académica de apelación ni se habilitan nuevas cuentas ADMIN.

Cada operación vuelve a leer estado y hash bajo el mismo bloqueo de conversación/estado que usa la publicación. Una edición, aprobación o protección concurrente impide limpiar una versión que dejó de ser elegible. La escritura de contenido y su evento se confirman juntas; ante error se revierten. No se registran textos ni archivos en los logs. Evaluaciones automáticas y protecciones cerradas también se minimizan a los noventa días; una protección abierta lo suspende.

## Operación

`python manage.py retain_content` muestra una vista previa sin escrituras. La aplicación requiere tanto `--apply` como `MAIN_RETENTION_APPLY=jery-2026-10-27-v1`. `--watch` repite lotes; intervalo predeterminado una hora, mínimo un minuto. Cada sección procesa como máximo 100 filas por defecto (máximo configurable 500), de modo que no recorre todo el contenido en una transacción. Los elementos protegidos se excluyen antes del límite para no bloquear los demás.

En Windows, `scripts/Start-Retention.ps1` recibe el JSON privado de arquitectura y Python. El campo privado retention_protected_password contiene la contraseña DPAPI del usuario SQL dedicado main_retention; no copiar a Git. Sin opciones ejecuta una vista previa, con -Apply aplica una pasada, con -Apply -Background repite cada hora en proceso oculto. La aplicación web no recibe permisos de borrado; el proceso separado no recibe contraseñas de usuarios, cambios de identidad ni permisos de creación de tablas. Modelo de permisos: database/retention-grants.sql. El operador de base provisiona la cuenta y aplica las concesiones mínimas después de V15.

El proceso se detiene al cerrar Windows; no se configura como servicio permanente. El despliegue futuro debe supervisarlo y alertar ante fallo. Revisar var/retention/worker.stdout.log y worker.stderr.log. La salida indica política, modo y contadores; no supone que una compilación o el mero inicio del proceso haya limpiado datos.

## Verificación

63 pruebas Django aprobadas, incluidas seis de límites, edición reciente, archivos publicados, protección, CSRF, reversión e idempotencia, más dos de listado y caducidad de metadatos. Diez pruebas de integración Spring de notificaciones/recordatorios aprobaron con V15 en H2. El primer intento usó nombres de clases incorrectos y no ejecutó casos; se corrigió y esa ejecución no se cuenta como prueba aprobada.

scripts/verificar_retencion_mysql.py comprobó doce condiciones en MySQL con fixtures sintéticos dentro de una transacción revertida. Incluye vista previa, bloqueo por apelación, huella cambiada, publicación/archivo preservados y eliminación de bytes de un estado pendiente caducado. No hubo proveedor externo ni borrado de contenido real en esa verificación.

Se hicieron además cuatro comprobaciones con la cuenta dedicada: borrado de mensaje sintético y estado sintético, rechazo al intentar modificar una identidad y vista previa de lote. El primer ensayo detectó que MySQL necesita SELECT sobre el identificador usado en el WHERE de DELETE; se concedió solo esa columna en las tablas de bytes, sin lectura del payload. Los fixtures desechables se retiraron con sus identificadores exactos.

Se comprobó en navegador el flujo de protección y resolución de un estado sintético privado del ensayo anterior; no se publicó y no quedó protección abierta. Capturas: informes/2026-10-27-revision/retention-hold-active.png y retention-hold-resolved.png. El proceso local se inició en modo apply cada hora y se verificó su primer lote sin errores, con cero elementos elegibles y cero eliminados. Esto acredita arranque y una pasada; no acredita disponibilidad permanente del futuro despliegue. El proceso debe reiniciarse tras cerrar Windows.
