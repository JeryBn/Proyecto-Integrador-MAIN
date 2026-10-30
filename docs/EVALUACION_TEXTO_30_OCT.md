# Evaluación piloto de moderación textual al 30 de octubre

Avance de M20 sin servidor público. Se evaluaron con el proveedor real siete ejemplos sintéticos aportados por Pamela Regina Meneses Mayhua en docs/revision/pamela/CASOS_M20.csv. Se conserva su archivo y autoría originales. Las rutas de referencia se interpretaron conforme a las categorías iniciales aprobadas por Jery; todavía no son una validación humana independiente de cada etiqueta.

| Caso | Contexto sintético | Ruta de referencia | Resultado real |
|---|---|---|---|
| M20-01 | Actividad académica | CLEAR | CLEAR |
| M20-02 | Educación en salud apropiada | CLEAR | CLEAR |
| M20-03 | Denuncia de acoso al tutor | CLEAR | CLEAR |
| M20-04 | Acoso dirigido | HUMAN_REQUIRED | HUMAN_REQUIRED |
| M20-05 | Amenaza dirigida | HUMAN_REQUIRED | HUMAN_REQUIRED |
| M20-06 | Cita educativa de una agresión verbal | CLEAR | CLEAR |
| M20-09 | Prevención de retos peligrosos | CLEAR | CLEAR |

Las siete solicitudes respondieron con omni-moderation-latest: siete coincidencias con esas referencias, cero casos de riesgo de referencia enviados como CLEAR y cero casos claros de referencia derivados a revisión humana. No se publica contenido, rechaza usuarios, procesa la cola, activa el trabajador ni cambia la política mediante este evaluador. HUMAN_REQUIRED solicita revisión humana; no es un rechazo automático. CLEAR en el piloto tampoco constituye una publicación.

Los ejemplos M20-07 y M20-08 son marcadores descriptivos sin material de prueba: no se usaron para fingir cobertura sexual, discriminación o violencia gráfica. M20-10 es un caso de límites de archivos; M20-11 y M20-12 son flujos de revisión y apelación, comprobados por otras pruebas. No son casos válidos para medir clasificación textual en esta ronda.

Reproducción: scripts/evaluar_moderacion_texto.py muestra primero una vista previa; --run realiza como máximo siete solicitudes secuenciales con la clave existente en memoria y detiene la tanda ante un fallo del proveedor. Los textos proceden exclusivamente de los IDs permitidos del CSV; no consulta la base ni acepta contenidos de usuarios. La evidencia guarda IDs, huellas, categorías y rutas: docs/informes/2026-10-30-evaluacion-texto/results.json.

Pendientes de M20: confirmar las etiquetas con los revisores, ampliar una muestra representativa de categorías y contextos, evaluar imagen/audio/video y registrar falsos positivos y negativos sobre esa referencia humana. Siete ejemplos no acreditan exactitud representativa ni cobertura multimodal completa. También sigue pendiente el primer cambio personal de contraseña e ingreso de los titulares; no se realiza ni atribuye ese acto personal mediante una prueba técnica.

La moderación textual real quedó disponible en esta ronda; el asistente académico devolvió por separado AI_QUOTA_EXHAUSTED. No se trasladan los resultados de un servicio al otro.
