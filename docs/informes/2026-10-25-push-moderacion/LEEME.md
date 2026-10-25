# Incremento de push y moderación del 25 de octubre

Guía y configuración exacta: [PUSH_Y_MODERACION_25_OCT.md](../../PUSH_Y_MODERACION_25_OCT.md).

Se incluyen metadatos Firebase públicos, resúmenes de pruebas, respuestas sanitizadas de los dos probes OpenAI sintéticos, ocho comprobaciones MySQL/Django/Spring con proveedor controlado y cinco comprobaciones HTTP de la puerta de configuración push. No se incluyen tokens Firebase, claves, JSON de cuenta de servicio ni contenidos reales de usuarios.

`results.json` diferencia ejecuciones de casos distintos; `automatic-mysql.json` no es una prueba real de OpenAI. Los probes `moderation-text.json` y `moderation-image.json` sí llaman a OpenAI, con texto educativo e imagen generada, sin publicar ni cambiar BD.

Trello actualizado y comprobado:

- M10 pasó de BACKLOG MAIN COMPLETO a PRUEBAS. La lista nueva Push Android 25 de octubre tiene 4/4 comprobaciones técnicas marcadas. La tarjeta global sigue pendiente de credencial FCM del servidor, entrega real y dispositivo físico.
- M19 pasó de BACKLOG MAIN COMPLETO a EN PROGRESO. La lista nueva Moderación 25 de octubre tiene 4/4 comprobaciones técnicas marcadas. La tarjeta global sigue pendiente de política/evaluación y análisis automático completo de audio/video; imágenes requieren humano.
- M11, M20 y M15 recibieron comentarios con resultados, límites y decisiones faltantes.
- Fechas históricas y responsables originales conservados. Los 100 % de las dos listas técnicas no son 100 % de M10/M19 ni del proyecto.

Las capturas de Trello muestran el estado verificado de esas listas y tarjetas. El backend local permanece disponible con push desactivado hasta configurar el acceso de servicio; no se activó un trabajador sobre contenido real. El proyecto Firebase MAIN está separado de ETERNA y no se activó facturación.
