# Migración administrativa a Django — 22 de octubre de 2026

Continuación asistida solicitada por Jery Becerra Ninaquispe. Se conserva el trabajo original de Michael Julian Fernandez Camarena (contratos Spring) y Pamela Regina Meneses Mayhua (interfaz React).

## Incremento: cursos, matrículas y auditoría

- Django implementa directamente `/api/admin/v1/courses`, edición por ID, listado/cambio de matrículas y consulta de auditoría. No actúa como proxy de Spring.
- Mantiene el contrato React, autenticación administrativa, CSRF, paginación, docente activo, versión optimista del curso, bloqueo de reasignación con horario y operaciones de matrícula idempotentes.
- Cambios y auditoría se guardan en una transacción. La auditoría es de solo lectura por API; no expone hashes ni claves.
- Flyway conserva la propiedad del esquema de negocio; no se recrearon tablas ni se borraron datos existentes.
- Verificación: 20 pruebas Django correctas en SQLite desechable, incluidas cinco nuevas; 13 comprobaciones HTTP reales Django → MySQL → Spring. Se verificó que una matrícula nueva aparece y su revocación desaparece de la agenda de Spring en la misma sesión.
- Evidencia: `docs/informes/2026-10-22-django/academic-http.json`. El curso sintético de esa evidencia permanece para inspección; la matrícula de prueba quedó revocada.

## Pendiente en esta migración

Cuentas y revocación de sesiones entre servidores, parentescos, contactos/reportes, horarios, notas administrativas y moderación de mensajes/estados con descarga segura de adjuntos. La navegación muestra únicamente los módulos ya implementados en Django. No se declara terminada M22 ni la paridad global M21.
