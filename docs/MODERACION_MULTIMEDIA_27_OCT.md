# Evaluación asistida de audio y video

Incremento posterior al informe del 27 de octubre. Django dispone ahora de extracción local y análisis asistido de audio/video, además del texto e imagen existentes. No se activa sobre la cola real y no constituye aprobación automática completa de medios.

## Cobertura implementada

- Entrada íntegra de mensaje o estado, limitada a 12 MB y 60 segundos. Se comprueba duración finita, dimensiones y presencia de una sola pista de audio/video; pistas adicionales o formatos incompatibles requieren revisión humana.
- FFprobe/FFmpeg locales, sin shell, sin interacción, sin protocolos de red ni manifiestos HLS. Procesos con plazo de 10/25 segundos, salida acotada y archivos temporales que se eliminan al terminar incluso ante error.
- Audio normalizado a WAV mono 16 kHz de hasta 2 MB; transcripción `whisper-1` por HTTPS fijo, sin redirecciones, y evaluación del texto con `omni-moderation-latest`. No se transmiten nombres de archivo originales, cuentas, cursos, contactos ni historial.
- Video: audio si existe y hasta 12 fotogramas PNG, muestreo cada cinco segundos, dentro de 640 por 640. Los clips cortos también producen un fotograma. Se evalúan las categorías de cada muestra y de la transcripción, además del pie de contenido.
- Audio/video siempre concluyen en HUMAN_REQUIRED. Una transcripción puede omitir palabras o sonidos y el muestreo puede omitir escenas breves y texto visible; el administrador debe revisar el medio completo. No se rechaza ni publica automáticamente por ese resultado parcial.
- Transcripción y fotogramas no se guardan en BD ni logs. El resultado persistente sigue siendo la derivación humana de la evaluación por versión/política; las categorías agregadas se usan en memoria. No hay nuevo panel de evidencia automática para el revisor en este incremento.

## Activación pendiente

Reutiliza la clave privada existente. Además de las puertas previas de política, revisor y retención, exige `MAIN_MODERATION_MEDIA_ENABLED=true` en el proceso Django. Mantenerlo desactivado hasta aprobar tratamiento externo y evaluar cobertura/coste. Rutas locales opcionales: `MAIN_FFMPEG_EXECUTABLE` y `MAIN_FFPROBE_EXECUTABLE`; si no se establecen, se buscan los binarios en PATH. No descargar binarios desde contenido de usuarios.

La caché de derivaciones previas se conserva; para revaluar con la cobertura nueva se necesita una nueva versión de política aprobada. Lease de multimedia de 15 minutos, máximo tres intentos por revisión/política, sin reintento inmediato tras caída del proveedor. Cada medio puede requerir una transcripción y hasta 13 consultas de moderación; usar tandas pequeñas y aprobar su presupuesto antes de operación. No se configura un trabajador periódico sobre contenido real.

## Pruebas

52 pruebas Django aprobadas: se añadieron nueve sobre límites, tracks, timeout, contrato de transcripción, cuota, categorías y protección contra publicación incluso si un proveedor controlado devuelve CLEAR. Comando: `manage.py test administration --settings=main_admin.test_settings` desde admin-django. Un primer comando desde la raíz no descubrió pruebas y no se contabiliza como aprobado.

`scripts/verificar_medios_local.py` genera tono WAV y video de color con audio, comprueba FFmpeg real, normalización, fotogramas y derivación humana con transcripción/clasificador controlados. No realiza solicitudes OpenAI ni cambia BD. El ensayo encontró dos problemas reales de FFmpeg: rango YUV de JPEG y ausencia de muestra en clips cortos; se corrigieron usando PNG RGB y redondeo del muestreo. Ambos medios pasaron el ensayo posterior. Evidencia en docs/informes/2026-10-27-multimedia/.

Quedan evaluación representativa humana y del proveedor real de transcripción, cobertura de OCR/escenas breves/sonidos no verbales, configuración institucional y aceptación. M19/M09/M11/M20 no están completas por este incremento.

## Fuentes oficiales consultadas

- https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create
- https://developers.openai.com/api/docs/guides/moderation
