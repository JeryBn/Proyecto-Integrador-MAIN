# Navegación web accesible al 30 de octubre

Avance M04 posterior a la corrección Android con texto al 200 por ciento. La navegación principal web ahora es un punto de navegación semántico, identifica un único destino actual incluso en Inicio sin hash, actividades, cursos y conversaciones, y conserva el enlace de salto al contenido.

Cambiar de sección por teclado actualiza el título de la pestaña y lleva el foco al contenido principal, identificado con el nombre de la sección. Las recargas de datos y las consultas periódicas no repiten ese movimiento ni apartan al usuario de un campo en el que está escribiendo. Se añadieron indicadores de foco visibles para enlaces, botones y campos, y una variante compatible con colores forzados del sistema.

## Comprobaciones reales

La interfaz compilada se sirvió temporalmente en 127.0.0.1:8288, con las peticiones de sesión y API dirigidas al Spring real de 8088 y su MySQL compartido. No se sustituyeron respuestas con datos simulados. Se utilizó la cuenta sintética vacio. La vista previa permite verificar el cliente nuevo sin detener los servicios que usan el JAR anterior; el empaquetado posterior debe incorporar los recursos compilados.

- El primer Tab alcanza Saltar al contenido principal; Enter enfoca el punto principal.
- Las cinco secciones se activan con Enter, actualizan el título y el destino actual y colocan el foco en el contenido.
- El siguiente Tab alcanza un control del contenido y muestra contorno visible de tres píxeles.
- Un campo de búsqueda conserva el foco durante las recargas periódicas; no se reinicia el recorrido con cada respuesta del servidor.
- Inicio, Chats, Estados, IA y Perfil se comprobaron a 320 píxeles; también Inicio a 640 y Cursos y notas a 1280. Las siete vistas no mostraron desbordamiento horizontal del documento. Perfil se volvió a medir con sus controles ya cargados.
- Compilación React y 37 pruebas web existentes aprobadas.

Evidencia: docs/informes/2026-10-30-accesibilidad-web/results.json e inicio-320-foco.png. Los resultados describen interacción y medidas reales del navegador; no se presentan como ejecución de un lector de pantalla. Una observación inmediata de Perfil se tomó antes del siguiente fotograma; se repitió esperando el foco efectivo y quedó comprobada.

M04 conserva la validación manual con lector de pantalla y zoom real del navegador, además de la aceptación visual del usuario/equipo. Un viewport estrecho verifica reflujo, pero no se denomina una prueba de zoom al 200 por ciento. El navegador usado para estas comprobaciones volvió a su tamaño normal.
