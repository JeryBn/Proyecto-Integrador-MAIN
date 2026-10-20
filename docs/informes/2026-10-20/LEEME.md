# Revisión del 20 de octubre de 2026

Informe preparado para Jery Becerra Ninaquispe con asistencia técnica. Word y PDF contienen el mismo informe detallado, revisado visualmente. La matriz identifica los responsables originales y sus pendientes y conserva los aportes encontrados de Michael Julian Fernandez Camarena y Pamela Regina Meneses Mayhua. No se presume que un integrante no haya trabajado fuera de las ramas observadas.

## Publicación

Todos los avances de esta revisión están integrados en `main`, por instrucción de Jery, sin push forzado ni reescritura de autores. Los commits se separan por incremento:

- `b99080e`: cursos, matrículas y auditoría privada; `c3c672d`: contrato administrativo.
- `7fc7948`: línea base local de rendimiento.
- `a43f820`: adjuntos privados, migración V9 y ocho casos nuevos de servidor.
- `195dad8`: revisión web de adjuntos con confirmación explícita.
- `07b190b`: envío, visores, cancelación y reintentos Android; actualización de descarga en el hilo de interfaz.
- `5fb2a8b`: selector real de documentos Android.

Los incrementos anteriores de cuentas, logout y notas conservan sus commits independientes y la autoría original reconocida en el informe.

## Verificación actual

- **96 pruebas de servidor**, sin fallos, errores ni omitidos. Incluyen ocho administrativas y ocho de adjuntos. `server-tests.json` resume cada clase; no se suman repeticiones.
- **Cinco casos conjuntos Android** de adjuntos, cancelación/reintento, estados y notas pasaron. Resultado en `android-adjuntos-estados-notas-cinco-casos.xml`.
- **Un caso del selector real Android** pasó: TXT elegido desde el proveedor del sistema, enviado a revisión y leído sin alterar sus bytes. Resultado en `android-selector-un-caso.xml` y captura 29.
- **Un caso administrativo Android** aprobado anteriormente el 20/10 comprobó matrícula y revocación; su captura muestra también el curso creado y editado desde React. En total se citan siete recorridos del 20/10, separados de las pruebas históricas del 18/10.
- Se conservan fallos intermedios en el registro de revisión: la publicación de una descarga fuera del hilo UI impedía reflejar el audio y se corrigió antes de la ronda conjunta exitosa. La primera pulsación automatizada del selector no abrió el proveedor; su acción semántica permitió verificar el recorrido real.
- Revisión web: vista previa de TXT, aprobación desactivada hasta confirmar revisión y resultado de aprobación comprobados en navegador.
- Línea base local: ocho sesiones y 160 lecturas, cero errores, mediana 9,29 ms y p95 14,54 ms; no es capacidad de producción.
- **IA comprobada nuevamente el 20/10**: `AI_QUOTA_EXHAUSTED`, sin respuesta generada exitosa. `openai-recheck-safe.json` contiene solo estado y mensaje seguro, sin clave.

## Trello y alcance

M03, M07, M08 y M13 están FINALIZADO técnicamente con evidencia y límites. Cinco tarjetas ya estaban finalizadas antes: nueve de las 42 observadas, sin convertir ese recuento en porcentaje de esfuerzo. M08 admite JPG/PNG, video y audio desde archivo hasta 60 segundos y TXT UTF8; no incluye PDF/Office ni grabadora interna. El alcance se registró en M01. La paridad web completa está pendiente en M21 y la aceptación física en M17.

Las capturas con títulos anteriores y el XML de dos casos son evidencia histórica de etapas de la revisión; los resultados finales son el XML de cinco casos y el del selector. La aceptación del profesor y del equipo se mantiene separada.

## Cómo probar

Servidor local http://localhost:8088. Portal académico `/`; administración `/admin.html`; revisión humana `/moderation.html`. Android Studio tiene abierto `mobile/` del checkout `main`; el emulador queda conectado al puerto 8088 y con el APK nuevo instalado.

Usar las cuentas sintéticas del entorno local. El alumno consulta sus notas por curso y el docente registra o edita las de sus matriculados. Para adjuntar, abrir un chat o grupo, pulsar Adjuntar archivo y elegir un formato admitido. El mensaje queda pendiente; el administrador revisa el archivo completo antes de aprobarlo y el destinatario solo entonces puede descargarlo. El 100 % de carga no significa que se haya aprobado.

## Pendientes y bloqueos

Consultar `../../REVISION_2026_10_20.md` y `matriz-tareas.json`. Faltan notificaciones en segundo plano, avatar, paridad web completa, moderación automática y apelaciones, entre otros requisitos. Módulos ausentes siguen siendo trabajo de desarrollo, no se atribuyen todos a falta de acceso del usuario.

Para los bloqueos externos se necesita cuota o saldo operativo de OpenAI; servidor y dominio autorizados; custodia de firma de entrega y teléfono físico; y revisor de apelaciones y plazo de conservación. No hay despliegue de producción ni APK firmado de entrega. Los archivos entregados no contienen claves ni bases privadas.
