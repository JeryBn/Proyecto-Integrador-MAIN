# Integracion de Pamela para la presentacion

Commit recibido: 8a8d9bbb92908463bbe43d46af7cdffb4aad572c, rama pamela/interfaz-criterios. Se conserva como padre de la integracion; no se modifica su autoria.

Conservado: componentes React con formato legible, etiquetas aria-label de detalle por titulo, mensaje de busqueda o filtro sin resultados, diseño CSS y navegacion que conserva busqueda y filtro al regresar del detalle.

Adaptado: redireccion por API 401 a /login.html, que es el formulario real del paquete integrado. Se conserva el backend con login personalizado, redireccion al listado y logout con CSRF. Se recompilan los assets desde el codigo fuente para evitar mezclar versiones.

node_modules y frontend/dist quedan fuera del seguimiento; las dependencias se recuperan con npm ci. El paquete ejecutable contiene el JAR y fuentes sin depender de esas carpetas. Se conserva el historial de Pamela; no se reescribe ni se elimina su rama.

Validacion local del 6 de octubre de 2026:
- Build React con esbuild: correcto.
- Maven package: 14 pruebas, 0 fallos, 0 errores.
- Navegador: acceso alumno, 3 actividades, etiquetas accesibles especificas, busqueda sin resultados y combinacion busqueda/filtro: correctos.
- Abrir detalle y regresar conserva la busqueda Primer avance.
- Logout regresa a login.html?salida y muestra confirmacion.
- Cuenta vacio: 0 actividades y mensaje de estado vacio.

Las actividades cuyo limite ya paso se muestran vencidas segun la hora real de Peru. No se cambian fechas ni reloj para la exposicion. El paquete sigue siendo el avance HU06/HU07; Django, Kotlin e IA del producto continuan pendientes.

Recorrido de exposicion: entrar con alumno, explicar resumen y listado, buscar Primer avance, abrir detalle, regresar y demostrar que mantiene la busqueda, combinar con Proximas para mostrar el mensaje sin resultados; cerrar sesion y entrar con vacio.

Para regenerar el ZIP: compilar React, mvn package y python scripts/crear_paquete_local.py. Los cambios de Pamela deben partir ahora del main integrado para evitar volver a incorporar archivos generados.
