# Verificación acumulada — 18 de octubre de 2026

| Comprobación | Resultado verificado |
|---|---|
| Maven verify, JDK21 | 66 casos, 0 fallos, 0 errores, 0 omitidos |
| Android unitarias | 3 casos, 0 fallos |
| Android instrumentadas, Medium_Phone Android17/API37 | 19 casos, 0 fallos, 0 omitidos; BUILD SUCCESSFUL |
| Compilación debug y lint | APK generado; 0 errores, 6 warnings y 3 hints |
| Llamada real a OpenAI | HTTP429, credit_balance_exhausted / insufficient_quota; sin respuesta generada |
| IA en la interfaz Android | Error real de saldo mostrado, reintento disponible; no se cuenta como generación correcta |
| Persistencia tras reinicio | Identidades, preferencias, parentescos, contactos, horarios, mensajes aprobados y avisos conservados |
| Formato de cambios | git diff a34efa6 --check sin errores |

Son 88 casos distintos aprobados. No se cuentan repeticiones de una misma prueba como casos adicionales.
La primera ejecución Android de esta revisión falló por pérdida del puente local y se detuvo; se restableció y se repitió el conjunto completo. La repetición correcta finalizó en 10m31s, incluyendo recompilación y herramientas de Android Studio abiertas. No se oculta ese intento fallido.

Cobertura nueva: contexto académico de IA sin comunicaciones privadas, idempotencia y límite diario, revocación de contexto familiar, errores del proveedor, control de membresía y revisión exacta, avisos solo tras aprobación, ausencia de duplicados, preferencias/bloqueos y lectura por destinatario.
Las pruebas de éxito del proveedor usan una sustitución controlada. La prueba real falló por saldo; todavía falta evaluar la respuesta académica real con la clave creada.
Los tests no prueban rendimiento sostenido, producción MySQL/HTTPS, funcionamiento de push, medios/estados ni teléfono físico.

Los warnings de lint corresponden a SDK/dependencias; la compilación no tiene errores de lint. No se actualizan dependencias indiscriminadamente para ocultar avisos.
Entorno de revisión: JDK21, servidor 127.0.0.1:8088, H2 persistente aislado y datos sintéticos. Emulador y ADB del SDK instalado. No se borró un AVD ni se usó información personal.

Commits locales:
- 9c1f734: backend, persistencia, revisión, avisos, IA y configuración de despliegue.
- 1136f9a: interfaz Android, agenda, chat, avisos, IA y pruebas.
No se hizo push ni se cambiaron tarjetas de Trello a completadas. Ver ESTADO_IMPLEMENTACION_2026_10_18.md para límites y pendientes por tarjeta.

El resultado de aceptación del usuario, del equipo y del profesor sigue pendiente.
