# Regularización técnica de las tareas del 12 de octubre

Revisión realizada entre la noche del 20 y la madrugada del 21 de octubre de 2026, hora de Bogotá, por encargo de **Jery Becerra Ninaquispe**. Este documento registra trabajo observado y verificado en el repositorio y Trello. La fecha del 12/10 es la planificación histórica de las tarjetas; esta regularización no se presenta como una entrega hecha en esa fecha.

## Aportes y cambio de arquitectura

Se conservan el React de **Pamela Regina Meneses Mayhua** (`8a8d9bb`, panel `a34efa6`) y el backend administrativo Spring de **Michael Julian Fernandez Camarena** (`b386291`, 13/10). El documento histórico de Michael registraba entonces una sustitución de Django por Spring. El 21/10 Jery volvió a proporcionar la arquitectura: administración React/Django, usuarios React/Kotlin/Spring Boot y base compartida. Por esa confirmación se implementó Django; su ausencia previa no se atribuye automáticamente a una omisión individual.

Los commits nuevos son ejecución asistida para Jery. No prueban una revisión personal de Pamela/Michael ni aceptación del profesor. No se asignan sanciones ni se evalúa trabajo que pudiera existir fuera de las fuentes consultadas.

## Brechas cubiertas por tarjeta

| Tarjeta y responsable original | Qué se encontró | Trabajo de esta regularización |
|---|---|---|
| T03 — Pamela Regina Meneses Mayhua | Listado, filtros y etiquetas ya implementados; checklist de revisión sin registrar. | Búsqueda por título/curso y filtros Todas/Vencidas/Próximas, sin coincidencias, carga lenta, error, reintento y cuenta vacía comprobados. Se corrigió que Reintentar no recuperaba el perfil después de un fallo inicial. |
| T04 — Pamela Regina Meneses Mayhua | Detalle y regreso ya existentes. | Campos y fecha Lima, apertura con Enter, regreso conservando búsqueda/filtro y actividad no disponible comprobados; mensaje de red traducido. |
| T05 — Michael Julian Fernandez Camarena | Pruebas de API previas existentes; faltaba registro de la revisión web pedida por la tarjeta. | Revisión asistida con alumno/otro/vacio, matrícula, entregadas excluidas, orden, filtros y 401. Se enlazan resultados nuevos, sin atribuir su ejecución a Michael. |
| T06 — Michael Julian Fernandez Camarena | Autorización de detalle implementada; faltaba registrar el recorrido de revisión. | Detalle propio/ajeno/inexistente, teclado, regreso y sesión vencida comprobados. Tres pruebas Java nuevas de sesión y acceso renovado. |
| T09 — Michael Julian Fernandez Camarena | API Spring administrativa y validaciones ya entregadas. Faltaba Django para cumplir el diagrama confirmado el 21/10. | Django con sesión ADMIN, catálogo, listado/detalle/creación, validaciones, duplicados, auditoría atómica y acceso directo a MySQL. Se conserva la API original. |
| T10 — Jery Becerra Ninaquispe y Michael Julian Fernandez Camarena | H2 local no acreditaba escritura Django y lectura Spring sobre una base compartida. | MySQL aislado con Flyway V1–V9, modelos Django sin DDL de negocio, sesiones propias, cuentas de DB limitadas, zona Lima y flujo cruzado. Dato conservado tras reiniciar MySQL y ambos backends. |
| T11 — Pamela Regina Meneses Mayhua | Panel React ya implementado; defecto histórico de salida corregido en `2e5684d`; ingreso desde página antigua aún podía fallar por CSRF. | Se renueva CSRF antes del ingreso, se reutiliza React con Django y se verifica creación desde navegador. Carga fallida/reintento y salida fallida sin confirmación falsa comprobados. |
| MICHAEL — Michael Julian Fernandez Camarena | Preparación Spring documentada; Django aún no estaba en el checkout. | Entorno Django 5.2.18, dependencias fijadas, servidor Waitress, contrato y 15 pruebas aprobadas. Pendiente acuerdo de HU administrativa y cierre humano. |
| PAMELA — Pamela Regina Meneses Mayhua | Su interfaz y commits existen. No se verificó su PC ni una revisión personal posterior. | Paquete y React actualizados y ejecutados en la PC de Jery; evidencias técnicas nuevas. No se marca como probado el arranque en la PC de Pamela ni un acuerdo personal con Michael. |
| HU06 y HU07 — los tres integrantes | Funciones integradas; aceptación conjunta sin registrar. | Se reúnen pruebas web/API y Android para los criterios técnicos. La aceptación conjunta continúa separada del resultado técnico. |
| EQUIPO — los tres integrantes | Alcance/capacidad y aceptación académica aún requieren acuerdo. | Se documentan arquitectura confirmada, contratos y evidencias. No se inventa consenso, capacidad ni aceptación del incremento. |

## Cambios publicados en main

- `13ba640`: renovación del token al ingresar desde una página antigua; cuatro regresiones del formulario y tres casos Java nuevos.
- `8a9f938`: recuperación completa de perfil/listado al reintentar, salida confirmada y mensajes de red comprensibles; fixture HTTP reproducible de estados de interfaz.
- `d1eb219`: Django administrativo con identidad/MySQL compartidos, integración React, auditoría, documentación y pruebas.
- `4ec140e`: prueba Android del registro creado en Django y su autorización por Spring.

## Resultados comprobados

| Comprobación | Resultado y límite |
|---|---|
| Maven verify sobre la suite del servidor | **99 aprobadas**, 0 fallos, 0 errores, 0 omitidas. Incluye las 96 existentes y 3 nuevas de sesión; la suite usa H2 de pruebas. |
| Formulario web de ingreso | **4 aprobadas** en Node: token renovado al enviar, fallo de red/reintento, fallo inicial y doble envío. |
| Django | **15 aprobadas** con SQLite desechable: roles, CSRF, sesiones/rotación/revocación, validación, duplicados, orden, detalle y rollback de auditoría. |
| Django y Spring sobre MySQL real | **19 comprobaciones HTTP aprobadas** al crear; **16 aprobadas después de reiniciar** MySQL y ambos backends. Se volvió a aprobar la verificación en los puertos finales. Son ejecuciones del mismo contrato, no 35 pruebas unitarias independientes. |
| Android conectado a Spring/MySQL | **3 aprobadas**: registro creado en Django visible al matriculado y oculto al ajeno; detalle con campos/fecha; rechazo ajeno/inexistente/sin sesión. Emulador, no teléfono físico. |
| React en navegador | Búsqueda/filtros, campos/fecha Lima, teclado/regreso, error/vacío, ingreso/salida entre pestañas y creación desde React a Django comprobados. Fallos de carga/reintento administrativo ejercitados con fixture sintético identificado. |

Evidencias seguras en [carpeta del 21 de octubre](informes/2026-10-21-sprint12/LEEME.md). Los siete recorridos Android del informe del 20/10 son históricos separados; no se presentan como repetidos sobre MySQL.

## Cómo queda abierto el proyecto

- Usuarios web y API Spring: `http://127.0.0.1:8088/`, con MySQL compartido.
- Administración React/Django del Sprint 1: `http://127.0.0.1:8090/admin.html`.
- MySQL aislado: loopback 3307, base `main_architecture_review`. No se modificaron bases anteriores de XAMPP/otros proyectos. Credenciales locales fuera de Git.
- Android Studio y emulador permanecen abiertos; la app usa la conexión local 8088 reenviada por ADB. Se reinstaló el APK tras las pruebas.
- La base H2 anterior permanece en `var/android-tests.mv.db` y tiene copia privada previa al cambio. No se afirma haber migrado sus conversaciones/estados/ensayos a MySQL: la nueva base contiene datos sintéticos de arquitectura.

## Pendientes que requieren decisión o aceptación

1. Identificar la **HU administrativa oficial** y registrar la conformidad del equipo/profesor con su contrato. La operación implementada es crear actividades por curso y permitir consulta solo al matriculado; no se inventa un ID oficial de HU.
2. Registrar la aceptación conjunta de HU06/HU07 y del incremento. Una revisión asistida no certifica participación personal ni ejecución en otra PC.
3. Migrar a Django los módulos administrativos posteriores (cuentas, vínculos, cursos/matrículas y moderación) y verificar paridad completa en M21/M22. Siguen disponibles en la administración Spring de compatibilidad; esta entrega cubre las operaciones administrativas del Sprint 1.
4. Los pendientes globales del informe anterior siguen vigentes: despliegue HTTPS, firma/aceptación en teléfono, saldo de IA, revisor y retención de moderación, notificaciones en segundo plano y demás paridad. No se bloqueó la regularización técnica esperando esos elementos.

Guía técnica y referencias: [administración Django](../admin-django/README.md). Las pruebas HTTP requieren cuentas sintéticas y escriben una actividad de ensayo; no ejecutar sobre datos reales.
