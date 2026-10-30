# Comprobación del asistente al 30 de octubre

M12 y S3-02 conservan un bloqueo externo comprobado. Una consulta educativa sintética de la cuenta de demostración sin cursos devolvió HTTP 503 y AI_QUOTA_EXHAUSTED, sin respuesta académica. No se enviaron expedientes, conversaciones o datos académicos de personas reales. Se reutilizó el acceso existente del servidor; no se creó otra clave ni se modificaron pagos o límites.

La comprobación separada del endpoint de moderación textual sí respondió CLEAR con omni-moderation-latest. Que moderación funcione no habilita la cuota del asistente. No se cambia el modelo ni se muestra una respuesta inventada para ocultar el bloqueo.

scripts/verificar_ia_sintetica.py reproduce una solicitud del asistente y, si existe la clave local, una prueba textual de moderación. Requiere MAIN_TEST_STUDENT_PASSWORD privada para la cuenta sintética vacio. La clave del proveedor se maneja únicamente en memoria; la evidencia guarda códigos, modelo y longitud de respuesta, nunca claves, cookies, tokens o respuestas personales. No procesa la cola real de moderación. Evidencia: docs/informes/2026-10-30-ia/results.json.

Para cerrar M12: el titular habilita saldo o cuota del proyecto existente y avisa cuando esté disponible; después se comprueba una respuesta académica y su recorrido en Android y web. No debe compartir la clave por chat. La prueba actual acredita el bloqueo, no el funcionamiento exitoso del asistente.
