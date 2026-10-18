# Traspaso incremental Android → Web

**Revisión posterior:** el [estado actual](../ESTADO_IMPLEMENTACION_2026_10_18.md) y los [contratos v1](../CONTRATOS_V1.md) incorporan identidades, agenda, chat, avisos y asistente. El contenido siguiente conserva el traspaso anterior de HU06/HU07.

Esta guía describe lo que existe hoy en Android para que Web pueda integrar módulos a partir de contratos compartidos. No declara paridad entre plataformas.

## Base disponible

Android usa Kotlin y Jetpack Compose en `mobile/`. El backend Spring Boot es la fuente de identidad y autorización. El cliente no decide qué actividades puede consultar ni calcula su vencimiento.

| Recorrido | Fuente/contrato actual | Estado Android |
|---|---|---|
| Inicio de sesión | `/login-token`, `POST /login`, `GET /api/perfil` y cookie de sesión | Implementado |
| Inicio | Perfil y `GET /api/actividades/pendientes` | Implementado con datos autorizados |
| Actividades | `GET /api/actividades/pendientes`; título, curso, vencimiento y estado | T07 implementado |
| Detalle | `GET /api/actividades/{id}`; el servidor responde 404 si no está disponible para la cuenta | T08 implementado |
| Cierre de sesión | `GET /api/csrf` y `POST /logout` | Implementado |
| Mensajes, grupos, medios, estados, IA y notificaciones | Contratos compartidos aún pendientes | Sin servicio conectado |
| Privacidad y controles de cuenta | Requieren autoridad del servidor y matriz M03 | Parcial: tema local y cierre de sesión |

## Flujo para cada incremento

1. Confirmar en M01/M02 la historia, los datos, permisos, errores y dueño del contrato.
2. Revisar una captura real y el commit de Android; registrar la versión del cliente usada.
3. Implementar la experiencia web con el mismo servidor y las mismas reglas de autorización. No copiar código Compose ni duplicar lógica de permisos en la interfaz.
4. Probar carga, vacío, error, reintento, sesión vencida y acceso de otra cuenta en Android y Web.
5. Registrar diferencias por plataforma, resultados reproducibles y evidencia. M24 requiere datos reales compartidos y pruebas de ambos lados.

## Reglas de integración

- Un cambio incompatible de API se coordina y documenta antes de integrarlo.
- Sesión y CSRF aplican a acciones protegidas. No se incluyen credenciales o cookies en ejemplos ni evidencias.
- Las vistas futuras no deben sugerir que mensajes, moderación, IA o estados ya funcionan hasta que exista un endpoint probado.
- En la moderación, el servidor conserva la decisión de aprobar/publicar; ningún cliente puede saltarse la revisión.
- Los roles, parentescos, membresías y audiencias se validan en el servidor, no mediante botones ocultos.
