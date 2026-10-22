# Evidencias de la migración administrativa del 22 de octubre

[Informe Word](MAIN_Migracion_Django_22_octubre_2026.docx) y [PDF de tres páginas](MAIN_Migracion_Django_22_octubre_2026.pdf), revisados visualmente página por página. Amplían el informe histórico del 21 de octubre y conservan nombres y autoría originales.

| Archivo | Alcance comprobado |
| --- | --- |
| server-104.json | 104 pruebas Maven, cero fallos/errores/omitidos; H2 desechable |
| django-32.txt | 32 pruebas Django; SQLite desechable |
| android-3.xml | 3 casos Android 17, servidor final y MySQL compartido |
| academic-http.json | 13 comprobaciones reales de cursos/matrículas/auditoría |
| identity-http.json | 17 comprobaciones reales de identidad/vínculos/sesiones |
| scheduling-grades-http.json | 15 comprobaciones reales de horarios/notas |
| moderation-http.json | 38 comprobaciones reales de revisión y privacidad |
| final-architecture-http.json | 16 comprobaciones de la actividad original después de reiniciar Spring |
| boundary-http.json | 6 comprobaciones del cierre administrativo Spring y acceso protegido Django |

Además pasaron las cuatro pruebas del formulario de ingreso web y la compilación React. Chrome comprobó comunidad, cursos/auditoría y rechazo de una revisión sintética en Django. La captura de navegador falló; no se incluye una imagen que simule ese recorrido. El primer intento móvil no encontró dispositivo; se repitió correctamente con emulador por software. Los conteos anteriores se mantienen por suite; no acreditan aceptación del profesor ni toda la paridad web/Android.

Identidades, cursos, mensajes y estados de prueba son sintéticos. No se incluyen bases, cookies, claves ni configuración privada. [Contratos y pendientes](../../MIGRACION_DJANGO_22_OCT.md).
