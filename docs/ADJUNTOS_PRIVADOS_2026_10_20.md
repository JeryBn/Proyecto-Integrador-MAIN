# Adjuntos privados de mensajes

Trabajo del 20 de octubre de 2026 por encargo de Jery Becerra Ninaquispe. Responsables originales de M08: Jery Becerra Ninaquispe y Michael Julian Fernandez Camarena. Se publica en `main`, conservando autores y commits anteriores.

## Contrato

- `POST /api/v1/conversations/{id}/messages`, multipart con `clientKey` UUID, `text` opcional y `file`. Requiere sesión activa, CSRF y permiso para enviar en esa conversación. Devuelve 202 y mensaje pendiente, sin entregarlo al receptor.
- El envío JSON de texto conserva la misma ruta y su contrato anterior.
- `GET /api/v1/messages/{id}/attachment?revision=N`: solo la revisión que esa cuenta puede ver, comprobando pertenencia y bloqueos. El emisor puede previsualizar sus borradores; cancelar retira ese acceso. El receptor requiere aprobación. No hay URL pública.
- `GET /api/admin/v1/moderation/messages/{id}/attachment?revision=N`: ADMIN activo y revisión pendiente actual.
- La aprobación de un mensaje con archivo requiere `attachmentReviewed: true`, además de revisión y hash exactos. La pantalla muestra el contenido y exige confirmación explícita del revisor. La validación técnica no evalúa categorías de contenido.
- Respuesta con `Cache-Control: no-store`, `X-Content-Type-Options: nosniff` y nombre genérico. TXT se descarga como archivo; medios se sirven para el visor autenticado.

## Formatos y límites iniciales

JPG/PNG se decodifican y se vuelven a codificar como JPEG. Video y audio pasan por el procesador FFmpeg existente, con duración máxima de 60 segundos. Entrada máxima de 12 MB; descarga procesada limitada a 16 MB. TXT admite UTF8 válido hasta 1 MB, sin controles binarios salvo tabulación y saltos de línea. No se admiten PDF, Office ni grabación de notas de voz dentro del chat en este incremento.

Cada cuenta dispone de hasta 100 MB procesados por ventana móvil de 24 horas, incluidos archivos rechazados o cancelados. No es una política de eliminación. Un archivo es inmutable; editar la explicación genera nueva revisión y conserva el archivo. Para sustituirlo, cancelar y enviar otro mensaje. La revisión se vincula al texto y al hash del contenido procesado. Los reintentos comparan también archivo original y tipo, y no crean duplicados.

V9 agrega metadatos y fragmentos privados en la misma base. Se comprueba tamaño y hash al leer. Se creó respaldo local antes de aplicar V9; ese respaldo no se publica. La purga espera la política de conservación pendiente.

## Android

Selector de documentos del sistema, archivo seleccionado con opción Quitar, explicación opcional, porcentaje de carga, cancelación de la espera y reintento con la misma clave. El 100 % representa bytes enviados; la entrega requiere confirmación del servidor y moderación. Cancelar la espera no garantiza retirar una solicitud ya recibida: la pantalla lo explica y el reintento evita duplicados.

Visor de imágenes, video y audio privados y vista previa/exportación de TXT. Se pausa al salir de primer plano. Se cancelan cargas al cerrar la conversación o destruir el modelo. El cierre de sesión limpia la conexión y las cookies. No se utiliza almacenamiento público para medios privados de reproducción.

## Evidencia y límites de cierre

Las 96 pruebas de servidor pasaron sin fallos, errores ni omisiones. Ocho casos nuevos cubren aprobación explícita, acceso ajeno y pendiente, revisión del texto, idempotencia, CSRF, formatos inválidos, imagen real, corrupción de almacenamiento, expulsión y cuota diaria.

Android compiló. Un recorrido instrumentado pasó envío y reintento de TXT, imagen real, video con audio y audio; comprobó invisibilidad antes de aprobar, descarga posterior del destinatario y los cuatro visores. La revisión web se comprobó con TXT sintético: aprobación desactivada hasta confirmar y respuesta de aprobación correcta.

Los dos casos de adjuntos pasaron tras corregir una carga intermitente al alternar visores, aislando el estado y la descarga por mensaje. Incluyen cancelación y reintento sin duplicados. La ronda conjunta de cinco casos de adjuntos, notas y estados pasó sin fallos. La publicación del resultado de descarga se trasladó explícitamente al hilo de interfaz. El caso del selector real de Android también pasó: eligió un TXT desde el proveedor del sistema, lo envió y verificó sus bytes y estado pendiente. El primer intento de pulsación automática no abrió el proveedor; la acción semántica del botón permitió comprobar el recorrido real. M08 se cerró técnicamente con los formatos iniciales registrados en M01. Teléfono físico y aceptación permanecen en M17; la paridad web restante está en M21.
