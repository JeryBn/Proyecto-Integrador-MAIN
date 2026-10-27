# Demostración y aceptación de MAIN

Guion del corte del 27/10/2026 para Jery Becerra Ninaquispe y el equipo. Ejecutar con cuentas sintéticas y registrar quién observa, versión/commit, entorno, resultado y evidencia por recorrido. No registrar contraseñas, tokens, cookies o datos reales de estudiantes. Esta preparación no acredita una demostración ya ejecutada ni aceptación.

## Preparación

1. Actualizar main y anotar el commit del código usado. Distinguir versión del código, JAR en ejecución y APK instalado; actualizar Git no actualiza automáticamente un proceso abierto.
2. Para una revisión local, iniciar MySQL 3307, Spring 8088 y Django 8090 con el iniciador compartido y configuración privada existente. Para entrega, sustituirlos por el destino HTTPS aprobado y registrar su versión. No exponer puertos de esta PC para simular un despliegue.
3. Disponer de dos alumnos de prueba, un docente, un familiar y un ADMIN. Crear contactos, matrículas y parentescos mediante la administración autorizada. Conservar las contraseñas fuera del acta. Preparar una actividad, una nota y un mensaje sintéticos.
4. Abrir web de usuario y administración Django en sesiones separadas. Conectar el Android de prueba al mismo entorno, verificar la URL y anotar modelo/API sin serie del dispositivo. Si solo hay emulador, identificarlo como tal.

## Recorridos

| Orden e historia | Acción observable | Resultado que se debe comprobar |
|---|---|---|
| 1 HU06 y HU07 | Alumno consulta pendientes, busca/filtra, abre detalle y regresa. Probar otra cuenta y una actividad ajena. | Solo matrícula autorizada; campos y regreso correctos; vacío explicativo; acceso ajeno rechazado. |
| 2 M05 y notas | Docente registra y corrige una nota por curso; alumno consulta desde Inicio. Probar otro docente/alumno. | Nota persistida y actualizada en ambos clientes; autorización por curso y matrícula; ningún acceso a notas ajenas. |
| 3 T09 T11 y M13 | ADMIN crea una actividad mediante React/Django; alumno matriculado la consulta en Spring/Android y web. | Persistencia compartida y contrato consistente. Sin rol ADMIN ni CSRF se rechaza escritura. |
| 4 M06 y M07 | Abrir privado autorizado y grupo con invitación; aceptar/rechazar y revocar un miembro. | Sin duplicar privado; invitación no concede mensajes; revocación retira acceso. Repetir las seis parejas de roles si se requiere aceptación manual. |
| 5 M08 M09 y M19 | Enviar texto y archivo sintéticos. Antes de revisar, consultar como receptor; revisar en Django; corregir un rechazo. | Pendiente privado; entrega solo tras revisión; versión/huella exactas; original rechazado oculto; corrección señalada. Familiar no publica estados. No afirmar análisis automático de audio/video. |
| 6 M06 y M10 | Interrumpir la red de prueba durante un intervalo acordado, restaurarla y refrescar. Probar cierre de sesión y otra cuenta. | Recuperación sin duplicar escritura ni mostrar datos de la cuenta anterior. Registrar duración real; la simulación de dos horas de la suite se informa aparte. |
| 7 M12 | Con cuota disponible, pedir una explicación académica sintética desde web y Android; cancelar/reintentar y cambiar de cuenta. | Respuesta del proveedor real, contexto autorizado y separación de cuentas. Un error de cuota se registra como bloqueo, sin sustituirlo por un mock aprobado. |
| 8 M10 | Con credencial FCM configurada, activar permiso y enviar un aviso sintético con app abierta, en segundo plano y cerrada. Luego probar logout, silencio, bloqueo y permiso denegado. | Recepción real autorizada y aviso genérico. Token obtenido o HTTP de FCM aceptado no bastan como evidencia de recepción. Recordatorios y destino correcto se prueban por separado. |
| 9 M17 y M18 | Verificar APK release, huella aprobada, instalación física y recorridos del alcance; registrar decisión por historia. | APK firmado correspondiente al commit probado; teléfono real identificado como tal; acta de aceptación del equipo/docente. |

## Acta por historia

Copiar este registro por recorrido; no dejar aprobaciones prellenadas.

- Historia y plataforma:
- Commit, versión de artefacto y entorno:
- Datos sintéticos y preparación:
- Persona que ejecuta y persona que observa:
- Pasos y resultado observado:
- Evidencia local o enlace al resultado sin secretos:
- Resultado: pendiente de ejecutar, aprobado, rechazado o bloqueado:
- Defecto reproducible y tarjeta correspondiente:
- Decisión de aceptación, persona y fecha:

## Condiciones para cerrar

Si falta firma, teléfono, HTTPS, cuota o política de moderación, registrar el recorrido correspondiente como bloqueado. El informe de avances puede entregarse con esa limitación, pero no acredita cierre funcional de todas las historias. La aceptación no se deduce de compilar ni de contar tarjetas. Conservar miembros y plazos históricos de Trello, enlazar cada commit separado y cerrar únicamente los criterios realmente comprobados.

Consultar el inventario y los bloqueos exactos en REVISION_Y_PENDIENTES_27_OCT.md y la configuración de push/moderación en PUSH_Y_MODERACION_25_OCT.md.
