# Notas para profesores y estados MAIN

## Recorridos en Android

- **Profesor:** Inicio → Mis cursos → Registrar y editar notas. Solo aparecen estudiantes matriculados del curso propio. Registrar nota pide evaluación, nota y escala máxima; Editar mantiene la evaluación y permite corregir nota/escala. Se aceptan coma o punto y hasta dos decimales. La app advierte si otra sesión cambió esa nota; cancela el diálogo y actualiza el curso antes de volver a editar.
- **Estudiante:** Inicio → Mis notas → tocar el curso. Las calificaciones guardadas por el profesor se consultan desde el servidor; no se calcula un promedio con escalas o ponderaciones inventadas. Actualizar agenda vuelve a consultar las notas.
- **Estados:** Mi estado, actualizaciones de contactos, aros de vistos/no vistos, crear texto con fondo de color, foto JPG/PNG, video y audio. Se pueden seleccionar archivos o abrir la cámara del dispositivo para foto/video. Visor con segmentos, avance automático, pausa al mantener pulsado, navegación anterior/siguiente, responder por chat y eliminar un estado propio con confirmación.
- Inicio, cursos, horario, tareas y chats privados se conservan. No se añadieron publicidad ni canales.

La referencia de interacción es [WhatsApp Status](https://www.whatsapp.com/status). Esta implementación adapta el recorrido a MAIN y sus reglas de revisión; no representa todas las herramientas de edición, stickers, música, reacciones, selección individual de audiencia o accesibilidad de WhatsApp.

## Permisos y privacidad

- Solo ALUMNO y DOCENTE publican. FAMILIAR puede ver un estado únicamente si es contacto autorizado incluido en su audiencia; no obtiene acceso automático por parentesco.
- Todo estado empieza PENDING. Otros usuarios no reciben su texto ni sus archivos hasta una revisión humana APPROVED. REJECTED permanece disponible solo para el autor durante su vigencia, con el motivo.
- La audiencia se fija al crear: CONTACTS (contactos autorizados existentes en ese momento) o PRIVATE (solo el autor). Nuevos contactos no obtienen estados anteriores. Perder la relación, bloquear en cualquiera de los sentidos, desactivar al autor o cambiar su privacidad general a PRIVATE revoca la consulta.
- Caducidad: 24 horas desde el envío, incluida la espera de revisión. Después no se permite consultar ni descargar, tampoco al autor. Eliminar borra texto y archivos y retira su acceso; quedan identificadores/hash y registros de revisión/vistas. **No se ha fijado todavía una política de conservación y purga de estados caducados o rechazados.**
- La app consulta actualizaciones cada 5 segundos mientras la sección está abierta. La API comprueba permisos en cada descarga. No se puede revocar retroactivamente una copia que alguien ya haya descargado o capturado.
- Las visualizaciones se registran una vez por cuenta al abrir un estado ajeno autorizado; el contador solo lo ve su autor. No hay contadores ficticios.
- Estados inmutables: sustituir contenido requiere enviar otro estado, que comienza en revisión. No existe edición que conserve una aprobación anterior.

## Multimedia y revisión

- Subida máxima: 12 MB; video/audio: 60 segundos. Imágenes: JPG/PNG hasta 20 megapíxeles, decodificación, aplicación de la orientación EXIF de cámara y nueva codificación sin metadatos. Videos: una pista de imagen de hasta 4 megapíxeles por cuadro y una de audio opcional. Audio: una pista. El servidor valida contenedor, duración y pistas y recodifica la secuencia completa y su audio a MP4/H.264/AAC o M4A/AAC.
- FFmpeg/ffprobe portátiles, versión 9.0.2, paquete verificado por SHA256. Se requieren en `MAIN_MEDIA_FFMPEG_DIRECTORY`. Máximo dos validaciones concurrentes; proceso limitado a 45 segundos. Se restringen protocolos y formatos para rechazar listas remotas y pistas no admitidas. No se ejecuta el nombre original del archivo.
- Archivos almacenados en fragmentos privados de la base de datos; no hay una carpeta pública de cargas. Descarga con sesión y permisos actuales, `Cache-Control: no-store` y tipo normalizado. En Android, reproducción desde caché privada temporal que se elimina al cerrar el visor.
- Revisión administrativa: `/moderation.html` tras iniciar sesión en Administración. La cola permite ver la imagen o reproducir **todo** el video/audio y revisar el texto. Se exige el hash exacto al aprobar; una revisión antigua o un estado vencido falla. La validación técnica no detecta por sí misma contenido sexual, acoso o violencia.
- **Moderación automática multimodal pendiente.** El proyecto OpenAI respondió `credit_balance_exhausted`. No se aprueba contenido automáticamente cuando falta IA. Falta que Jery indique revisor de apelaciones y plazo de conservación.
- Estos archivos corresponden a estados. Adjuntos multimedia en chats (M08) siguen pendientes.

## Contrato del servidor

- `GET /api/v1/teaching/courses/{courseId}/grades`: filas `{studentId,name,grades[]}` solo para el docente asignado.
- `PUT /api/v1/teaching/courses/{courseId}/grades`: `{studentId,assessment,score,maximum,expectedRecordedAt}`. Para alta desde Android, `expectedRecordedAt="NEW"`; para editar, se envía el `recordedAt` leído. Conflicto devuelve 409. Se conserva compatibilidad del contrato previo que omite este campo. La variante administrativa existente conserva sus permisos.
- `GET /api/v1/grades`: notas del estudiante autenticado por curso y matrícula.
- `GET /api/v1/statuses`: propios vigentes y ajenos aprobados autorizados; límite de 200 candidatos recientes por consulta. Paginación de alto volumen pendiente.
- `POST /api/v1/statuses`: multipart `clientKey` UUID, `text`, `background` hexadecimal, `audience`, `file` opcional. Requiere CSRF por cabecera y sesión. Hasta 30 estados por cuenta/24 horas; reintento idéntico no duplica. Reutilizar clave con otro contenido, caducado o eliminado devuelve 409.
- `GET /api/v1/statuses/{id}/media`, `POST .../{id}/seen`, `POST .../{id}/delete`: autorización actual; mutaciones con CSRF.
- `GET /api/admin/v1/moderation/statuses?offset=0&limit=20`, `GET .../statuses/{id}/media`, `POST .../statuses/{id}/review` con `{contentHash,approved,reason}`. Solo administrador. Rechazo requiere motivo.
- Flyway V7 añade estados, archivos privados, audiencia y vistas. No se modifica ni elimina el esquema académico existente. Producción MySQL aún requiere validación real.

## Pruebas y aceptación

Los resultados y capturas de la entrega se registran al terminar las pruebas. Cámara física, distintas marcas de teléfono, tamaños de letra grandes, alta carga y herramientas de edición avanzadas requieren validación adicional. No se cambia automáticamente el estado de tarjetas de Trello ni se presume aceptación del profesor/equipo.

### Resultado verificado

- Servidor: **80 pruebas**, 0 fallos, 0 errores, 0 omitidas; `teacher-status-api-final.log`. Incluye autorización de notas, conflictos de edición, estados privados, bloqueo, caducidad, borrado, CSRF, hash exacto de revisión, imagen falsa, las ocho orientaciones EXIF y procesamiento real de video/audio sintéticos con FFmpeg.
- Android: **6 pruebas de interfaz** en Medium_Phone / Android 17, sin fallos; `teacher-status-regression.log`. Incluyen registro/edición y consulta de la nota, imagen aprobada, texto pendiente sin filtración al contacto, video con primer cuadro renderizado, audio preparado/reproducido, Inicio, actividades, perfil, tema, chats privados y corrección de mensajes.
- **3 pruebas básicas** de configuración del servidor en la app; comprobación de compatibilidad Android (lint) sin errores. Los avisos no críticos de lint permanecen en el informe.
- Se repitieron los recorridos después de corregir el acceso a Estados y los selectores de las pruebas. No se suman las repeticiones como casos nuevos. Las capturas finales corresponden a la ejecución aprobada.
- No se verificó captura de cámara en teléfono físico ni salida audible en ese teléfono. Los clips probados son sintéticos. No se obtuvo una respuesta generada de OpenAI.
