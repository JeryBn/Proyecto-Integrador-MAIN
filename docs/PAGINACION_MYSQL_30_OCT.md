# Paginación y privacidad de chats al 30 de octubre

Avance M06 comprobado con peticiones reales a Spring y Django sobre MySQL local aislado. scripts/verificar_paginacion_mysql.py creó tres cuentas sintéticas, un grupo y 35 mensajes. Se aprobaron mediante revisión humana sintética 33; dos quedaron pendientes para comprobar el filtro de privacidad antes de paginar.

Resultado: 25 comprobaciones aprobadas. El receptor recuperó exactamente los 33 mensajes aprobados mediante cinco páginas de tamaño máximo siete, sin duplicados, borradores, motivo de revisión ni huella privada. El autor recuperó sus 35 mensajes, incluidos dos pendientes. Los 33 avisos de mensajes fueron únicos y también se recorrieron por páginas. Una tercera cuenta no pudo leer la primera ni una página posterior. Se rechazaron offset negativo, límite cero y límite superior a cien.

Tras retirar al receptor del grupo, tanto la primera página como las posteriores devolvieron 404 y sus avisos dejaron de ser visibles. Las tres cuentas sintéticas quedaron deshabilitadas al terminar. Se conservan únicamente fixtures identificados de esta base de pruebas; no se tocaron cuentas, grupos ni mensajes de personas reales. No hubo proveedor IA ni FCM.

El contrato de mensajes obtiene ventanas de los mensajes más recientes y devuelve cada ventana en orden cronológico para mostrarla en el chat. El primer ensayo esperaba erróneamente que concatenar páginas produjera una lista descendente; se corrigió el verificador según el contrato existente. Se comparó la secuencia final contra los identificadores y fechas devueltos durante la creación, sin modificar el orden del servidor ni los clientes. El intento fallido no se contabiliza como aprobado y su limpieza de cuentas también se ejecutó.

Reproducción explícita: iniciar los servicios locales de pruebas 8088/8090, configurar MAIN_TEST_ADMIN_PASSWORD de forma privada y ejecutar el script. La evidencia no guarda contraseñas, cookies, tokens, cuerpos de mensajes ni identidades de usuarios reales: docs/informes/2026-10-30-carga/pagination.json.

Este resultado amplía la comprobación real de MySQL sobre los ensayos anteriores de roles y reconexión. No acredita red móvil física, entrega con la aplicación cerrada, push, servidor público ni aceptación del equipo. Esos criterios conservan su estado propio en M10/M15/M16/M17.
