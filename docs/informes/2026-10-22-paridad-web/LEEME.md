# Evidencias de paridad web — 22/10/2026

Trabajo asistido solicitado por Jery Becerra Ninaquispe. Conserva las autorías originales y la asignación de Pamela Regina Meneses Mayhua (web), Michael Julian Fernandez Camarena (backend/administración/pruebas) y Jery (Android/coordinación) registradas en Trello. Estas pruebas no equivalen a aceptación del equipo/profesor.

## Resultados finales

| Verificación | Resultado y archivo |
| --- | --- |
| Spring / autorización / moderación / persistencia | 107 casos, 0 fallos: `spring-107.log` |
| Django administrativo | 32 casos, 0 fallos: `django-32.log`; SQLite desechable |
| Cliente web | 19 casos, 0 fallos: `client-19.log` |
| Android real con MySQL compartido | 9 casos distintos, 0 fallos en la ronda final: `android-9.log` |
| Puente multimedia ampliado | Repetición de WebParityBridgeTest aprobada: `android-cruce-multimedia.log`; no es un décimo caso distinto |
| Navegador real | 39 comprobaciones registradas: `browser-checks.json` |
| Tras reiniciar Spring con el JAR empaquetado | 33 comprobaciones aprobadas: `after-restart.json`; incluye hash de assets, autorización de lectura/escritura/logout y persistencia |
| Proveedor IA real | Respuesta de cuota/saldo agotado; captura 07. No se certifica generación exitosa |

Los tres primeros intentos de la suite Android detectaron: origen administrativo antiguo, revisión multimedia sin confirmación de archivo, espera de audio muy breve y teclado sobre el botón de envío. Se corrigió el soporte de pruebas: Django 8090, revisión explícita, apertura determinista del audio y cierre del teclado. La ronda final de nueve casos pasó. No se ocultan esos intentos como si hubiesen aprobado desde el inicio.

## Capturas reales

01 Inicio académico; 02 tema oscuro (restaurado después); 03 corrección pendiente; 04 mensaje aprobado y respuesta recibida; 05 grupo/invitaciones; 06 estados pendientes; 07 error real de IA; 08 nota editada por docente; 09 visor de video; 10 chat a 390 px sin desborde horizontal; 11 acceso ajeno denegado; 12 mensaje Android recibido en web; 13 estado Android en visor web.

La fotografía enviada desde Android se comprobó cargada en Chrome con dimensiones reales 8×8 después del reinicio. El video y audio web cargaron controles/metadatos sin reproducción automática para no interrumpir al usuario. Los visores/reproducción Android se comprobaron en el emulador. Las capturas de esta carpeta corresponden a los momentos indicados, no a una declaración de aceptación visual definitiva.

## Reproducibilidad y límites

[Guía de pasos, contratos y fixtures](../../GUIA_PARIDAD_WEB_ANDROID.md). Soporte: `scripts/preparar_paridad_web.py`, `scripts/verificar_paridad_web.py` y `WebParityBridgeTest`. Las cuentas y textos usados son sintéticos. Las claves y cookies no se publican. Los mensajes y archivos pendientes no se entregan; la revisión se hizo solo sobre fixtures controlados.

No incluye éxito de OpenAI, carga sostenida, teléfono físico, todos los navegadores, red móvil prolongada, captura cámara/micrófono web, push con la app cerrada o aceptación del equipo. La web sube archivos seleccionados; Android conserva captura nativa. El informe Word/PDF de migración Django es un corte histórico distinto y no contiene este incremento.

Carpeta android/: diez capturas extraídas del último ciclo completo aprobado del emulador (notas, estados, adjuntos y detalle Django/MySQL). trello-corte.json registra M21, M23 y M24 en PRUEBAS y las siete tarjetas actualizadas. Los logs se incluyen expresamente porque la regla general de Git los ignora.
