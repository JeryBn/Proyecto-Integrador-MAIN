# Aplicación Android de Jery — HU06/HU07

Solicitud vigente del 12/10/2026: desarrollar solo la aplicación y sus complementos, publicar un commit por tarea y registrar avance real en Trello. Se preservan los aportes de React y el trabajo pendiente de Django.

## Entregables por tarjeta

| Tarjeta | Entregable de Jery | Dependencia | Criterio de salida |
|---|---|---|---|
| Preparación Kotlin | Proyecto Android, sesión y conexión documentadas | T01/T02 y backend disponible | Compilación, pruebas de login/logout/CSRF y evidencia; acuerdo de alcance queda para equipo/profesor |
| T07 | Listado nativo de pendientes | Preparación y GET pendientes | Alumno autorizado, cuenta vacía, carga/error/reintento, compilación y captura |
| T08 | Detalle y regreso nativos | T07 y GET detalle | Campos, 404/401, contexto preservado, compilación y captura |

Las HUs padre tienen validación del equipo. T10 continúa como trabajo conjunto Jery/Michael; este incremento consume la API existente y no decide migraciones ni configura Django.

## Supuestos de implementación

- Backend local del repositorio: 127.0.0.1:8088; H2 y cuentas sintéticas para evidencia.
- Kotlin nativo con Compose, minSdk 26; proyecto en mobile/ independiente del backend Maven.
- La autorización, pertenencia a cursos, orden y vencimiento provienen del servidor.
- Autenticación actual por cookie/sesión y formularios con CSRF, sin JWT.
- Conexión de desarrollo por adb reverse, sin ampliar escucha del servidor a la red.
- Estado en memoria, conservado en rotación; credenciales y cookies no se guardan en almacenamiento.

## Aceptación técnica

1. Login correcto muestra identidad; credenciales incorrectas no reutilizan otra sesión.
2. Pendientes solo del alumno, sin entregados, conservando orden de fecha/ID.
3. Estado vacío y fallos comprensibles; reintento funcional.
4. Selección abre detalle por ID; ajena/inexistente no revela datos.
5. Regreso conserva búsqueda/filtro; Back de Android debe tener el mismo comportamiento.
6. Sesión expirada vuelve a login y borra datos de la cuenta anterior.
7. Logout invalida la sesión con CSRF y borra estado local.
8. APK debug compilado, análisis Android sin errores relevantes y pruebas en emulador contra el backend real.

## Secuencia

Preparación → T07 → T08 → verificación integrada y entrega. Los commits registran cambios reales; no se vuelven a atribuir APIs ya finalizadas. Cada reporte incluye fecha, comandos reproducibles, resultados y límites. La aceptación por Michael o Product Owner no se simula.
