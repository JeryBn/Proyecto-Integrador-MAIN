# Resumen orientativo para la revisión administrativa

Incremento posterior a la extracción multimedia. La cola Django muestra ahora resultado, cobertura, categorías detectadas, número de muestras y duración correspondientes a la huella exacta del mensaje o estado pendiente. No almacena ni devuelve transcripciones, fotogramas, claves o respuesta cruda del proveedor. Un contenido editado no reutiliza el resumen de la versión anterior.

Flyway V14 añade metadatos a automatic_assessment. La publicación sigue separada: HUMAN_REQUIRED y cualquier imagen/audio/video permanecen privados y requieren revisión completa. El resumen no marca por sí mismo la confirmación de archivo revisado. Se corrigió además el botón de aprobar estado para mantenerlo desactivado hasta esa confirmación y ante errores del medio.

Pruebas: 55 Django aprobadas; diez de Spring de avisos/recordatorios y empaquetado aprobados con V14; ocho comprobaciones MySQL/Django/Spring reales con video sintético y proveedor controlado. Se verificó la interfaz administrativa real y su botón de aprobación desactivado. No se hizo una nueva solicitud OpenAI para esta evaluación controlada.

El primer ensayo intentó crear un estado usando el usuario SQL limitado de Django y fue rechazado correctamente por permisos. Se corrigió el ensayo para crear el estado por la API de usuarios Spring con sesión/CSRF; no se ampliaron permisos de base. La asignación automática continúa sin activarse sobre contenido real mientras no estén configuradas las cuentas de revisión y el proveedor.

Por confirmación expresa de Jery del 27/10, el alcance técnico de T09/T11 es administrar actividades (crear, listar y consultar detalle) con React → Django y datos compartidos con Spring/Android. Esto permite registrar ese acuerdo técnico; no acredita aceptación del docente ni una prueba personal de Pamela o Michael.

Una nueva consulta real del asistente académico con una cuenta sintética sin cursos devolvió HTTP 503, código AI_QUOTA_EXHAUSTED. No hubo respuesta académica; el bloqueo actual de cuota queda comprobado y no se resuelve creando otra clave o simulando la respuesta.

La política de moderación se definirá conforme a la delegación de Jery: responsable Jery Becerra Ninaquispe y suplente Michael Julian Fernandez Camarena; pendientes siete días, rechazados treinta días, auditoría noventa días. La designación de personas no habilita por sí sola cuentas ni permisos nuevos. La puesta en marcha y el borrado se implementan y prueban por separado, sin eliminar contenido real en esta ronda.
