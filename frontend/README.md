# Web React de MAIN

Desde esta carpeta: npm ci y npm run build. El script genera dist/, copia assets al backend y actualiza su index.html. Luego ejecutar mvn package en la raíz del proyecto y copiar el JAR generado a la raíz antes de usar INICIAR-MAIN.cmd.

La web se sirve desde el mismo origen del backend; no necesita un segundo servidor en la demostración. App gestiona estado de búsqueda/filtros, ActivityCard representa tarjetas y ActivityDetail solicita un detalle autorizado. El formulario auxiliar de login continúa siendo HTML con protección CSRF.

Los datos provienen de la API, no de un listado fijo incrustado en React. El control de acceso se aplica en Spring Boot.
