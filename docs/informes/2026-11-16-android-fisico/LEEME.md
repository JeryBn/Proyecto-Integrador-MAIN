# Verificación Android física — 16 de noviembre de 2026

## Entorno

- Dispositivo autorizado por Jery: Motorola moto g86 power 5G.
- Android 15, API 35; pantalla física 1220 × 2712 y densidad 450.
- APK `debug` y APK `release` firmado generados desde `feat/institutional-experience`.
- Spring local saludable en `127.0.0.1:8088`, conectado al teléfono mediante `adb reverse`.
- Solo se usaron cuentas y contenido sintéticos de MAIN. No se guardaron identificadores del dispositivo, tokens, archivos ni cuentas personales.

## Resultados comprobados

- Instalación mediante ADB y arranque frío correctos, sin cierre inesperado ni ANR.
- Acceso real con `alumno`, carga del inicio y tres actividades sintéticas.
- Inicio, Chats, selector de contactos, Estados, compositor, IA, Ajustes, Privacidad y Apelaciones accesibles.
- Cámara del sistema y selector multimedia invocados correctamente. La evidencia del selector se eliminó porque Android mostraba archivos y cuentas personales; no se copió al repositorio.
- Permiso de notificaciones solicitado por Android y concedido; la app informó correctamente que aún falta Firebase.
- La IA informó de forma segura que no está configurada, sin exponer detalles técnicos.
- Tema oscuro, recordatorios, segundo plano/reanudación, rotación horizontal y recuperación del puente local verificados sin caída.
- Texto al 200 %: etiquetas primarias sin recorte; la barra inferior se desplaza horizontalmente y permite alcanzar Ajustes. El tamaño se restauró a 1.0.
- Cinco pruebas instrumentadas de navegación, cuenta vacía, búsqueda, error de conexión y recuperación: 5/5 aprobadas en el teléfono.
- Prueba instrumentada adicional de fuente al 200 %: 1/1 aprobada en el teléfono.
- Se creó una identidad release RSA 3072 nueva fuera de Git, protegida con Windows DPAPI. El APK firmado del commit `36e2d6a` pasó `apksigner`, conservó `pe.tecsup.main.mobile`, no es depurable y fue instalado en el teléfono después de retirar únicamente la variante debug de QA.
- La variante release rechazó `http://127.0.0.1:8088` y mostró la instrucción segura de usar HTTPS. Huella pública SHA-256 del certificado: `250bdbefd60597b2be57c0020fd67d3c87a61a0fdbf47ee439251c48cccfd230`.

La primera ejecución halló una expectativa antigua en `PendingUiTest`: buscaba un mensaje anterior al manejo profesional de errores. La aplicación ya mostraba `No encontramos el servidor. Revisa tu conexión y la dirección configurada.`. Se actualizó la expectativa y se repitieron tanto el caso aislado como la ronda completa, ambos sin fallos.

## Límites que permanecen abiertos

El 19 de noviembre se verificó el inicio de sesión desde el APK firmado en el Motorola con un enlace HTTPS temporal de Cloudflare y la cuenta sintética de alumno. La pantalla Inicio cargó tres actividades pendientes del servidor. También se verificó por HTTPS el acceso web de alumno y docente. El ensayo cubre el portal de usuarios con la base de demostración en memoria. Quedan pendientes el entorno administrativo compartido y las notificaciones push.

El 19 de noviembre se instaló el APK release de `2905d07` encima del release anterior en el mismo Motorola. Android aceptó la actualización, conservó la fecha de instalación inicial y el directorio de datos, y abrió MAIN correctamente. La prueba de inicio de sesión contra el servidor público y la entrega de notificaciones quedan pendientes.

- La identidad release existe y funciona en esta cuenta de Windows. Antes de distribuir el APK falta guardar una copia externa del `.p12` y una contraseña recuperable en un gestor privado; DPAPI por sí solo no permite recuperar la clave en otra computadora.
- Firebase Android y la cuenta FCM del servidor no están configurados, por lo que no se puede acreditar push con la app abierta, en segundo plano o cerrada.
- No se capturó ni almacenó foto, video o audio personal. La grabación y el envío real deben repetirse con material sintético autorizado cuando exista el entorno HTTPS final.
- El entorno H2 de demostración no contiene contactos autorizados, por lo que el envío entre dos cuentas debe repetirse contra la base final.
- IA real, moderación del proveedor y transcripción continúan bloqueadas por cuota/configuración externa.
- TalkBack y aceptación visual requieren una persona que escuche y confirme el recorrido; la prueba de semántica y fuente grande no sustituye esa aceptación.

Esta ronda acredita instalación debug, funcionamiento físico y una instalación release no depurable con exigencia HTTPS. No acredita todavía funcionamiento release contra el servidor final, servicios externos ni aceptación académica.
