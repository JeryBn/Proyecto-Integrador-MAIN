# Evidencias de regularización del Sprint del 12 de octubre

Ejecución entre 20 y 21/10/2026 para Jery Becerra Ninaquispe. [Informe factual por tarjeta](../../REGULARIZACION_SPRINT_12_OCT.md). Se conservan autores originales; no se acredita revisión personal de otros integrantes ni aceptación académica.

[Word](MAIN_Regularizacion_Sprint_12_octubre_2026.docx) y [PDF](MAIN_Regularizacion_Sprint_12_octubre_2026.pdf) de seis páginas, revisados visualmente. `verificacion-informe.json` identifica resultados, límites y hashes de ambos documentos.

- `architecture-http.json`: 19 comprobaciones sobre Django y Spring/MySQL al crear una actividad sintética.
- `architecture-http-after-restart.json`: 16 comprobaciones del mismo registro tras reiniciar MySQL y los dos backends.
- `architecture-canonical.json`: nueva ejecución de las 16 en los puertos finales 8090/8088. No sumar ejecuciones repetidas como casos distintos.
- `django-15-casos.txt`: prueba Django con SQLite desechable; el flujo MySQL real se acredita aparte con los JSON.
- `acceso-web-4-casos.txt`: cuatro regresiones del formulario web.
- `android-mysql-3-casos.xml` y `31-django-mysql-android.png`: tres casos aprobados en emulador contra Spring/MySQL.
- `01`–`04`, `06`, `08`, `11`–`14`: recorridos reales con cuentas sintéticas. `12`–`13` pertenecen al servidor H2 antes de cambiar a la arquitectura compartida; `14` es creación React/Django/MySQL.
- `05-error-red-anterior.png`: fallo anterior al mensaje de red traducido; no es el estado corregido.
- `07`, `09`, `10`, `15`, `16`, `17`: fixture HTTP sintético del React real para carga, error, reintento y salida sin confirmación falsa. No acreditan autenticación ni permisos de producción.
- `18`–`27`: actualización Trello, cuatro cierres técnicos T03–T06, condiciones de acuerdo aún pendientes y conteo de 13 tarjetas FINALIZADO. La fecha de ejecución es 21/10; no se presenta como entrega realizada el 12/10.

No se incluyen claves, cookies, tokens, archivos de configuración privada, respaldos de base ni datos reales de estudiantes. Los informes del 20/10 conservan su corte histórico; esta carpeta los complementa.
