# Navegación Android con texto ampliado al 30 de octubre

Avance M04. Con fuente de sistema al 200 por ciento, la navegación de cinco destinos disponía de poco ancho para las etiquetas. Una prueba instrumentada contra la versión anterior b95f5f9 reprodujo recorte visible de Estados. La barra ahora conserva una anchura mínima de lectura cuando la fuente es grande y permite desplazamiento horizontal para llegar a todos los destinos. Las etiquetas admiten dos líneas y su altura de texto no se recorta por la medida del contenedor. Con tamaño normal se conserva la barra existente.

Se mantiene Inicio con cursos, notas y tareas. Los destinos conservan sus nombres accesibles y los permisos de los servicios. La barra permite acceder a los cinco destinos sin reducir artificialmente el tamaño de letra elegido por el usuario.

## Comprobaciones

| Prueba | Resultado |
|---|---|
| Versión anterior y fuente 2.0 | Falla por recorte visible de Estados |
| Corrección y fuente 2.0 | Una instrumentada aprobada con acceso, etiquetas completas, desplazamiento, destinos y cierre de sesión |
| Fuente normal restaurada a 1.0 | Una instrumentada NavigationUiTest aprobada, con Inicio, secciones, tema, recreación y separación de sesiones |
| Unitarias Android | Cuatro aprobadas |
| Compilación debug y lint | Correctas; cero errores y doce advertencias lint |

La prueba mide los límites reales de las líneas renderizadas, texto completo y ausencia de elipsis. Tolera un píxel de redondeo: el indicador genérico didOverflowWidth de Compose puede activarse por fracciones de píxel sin recorte perceptible. Una primera expectativa demasiado estricta se corrigió antes del ensayo comparativo. La prueba corregida falla en la versión anterior y pasa en la nueva; no se oculta el defecto original con una tolerancia grande.

Entorno de esta ronda: emulador Medium_Phone, Android 17, API 37; servidores locales reales y cuentas sintéticas. Evidencia en docs/informes/2026-10-30-accesibilidad/results.json y capturas de Inicio/Ajustes al 200 por ciento. No se empleó un teléfono físico. El tamaño de fuente del emulador volvió a 1.0 al terminar.

Reproducción: establecer font_scale 2.0 en un emulador de pruebas y ejecutar LargeTextUiTest con puentes ADB 8088/8090; después restaurar el ajuste y ejecutar NavigationUiTest, testDebugUnitTest, assembleDebug y lintDebug. Los primeros intentos detectaron un problema de compilación del verificador y después el recorte; solo las rondas finales se contabilizan como aprobadas.

## Pendiente de la tarjeta global

M04 mantiene la validación con lector de pantalla, zoom web y aceptación visual del usuario/equipo. Un intento de captura del navegador a 320 píxeles no terminó correctamente y no se declara aprobado; el tamaño del navegador se restauró. Las pruebas Android que Jery indicó haber realizado con anterioridad deben enlazarse a su evidencia y distinguir teléfono de emulador. Esta ronda no modifica ni sustituye esa aceptación personal.

El APK debug recompilado contiene la mejora. Los APK release firmados de cortes anteriores siguen siendo artefactos de sus commits originales, no se les atribuye esta corrección.
