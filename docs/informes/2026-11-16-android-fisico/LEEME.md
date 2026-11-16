# Verificación Android física — 16 de noviembre de 2026

## Entorno

- Dispositivo autorizado por Jery: Motorola moto g86 power 5G.
- Android 15, API 35; pantalla física 1220 × 2712 y densidad 450.
- APK `debug` generado desde `feat/institutional-experience`.
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

La primera ejecución halló una expectativa antigua en `PendingUiTest`: buscaba un mensaje anterior al manejo profesional de errores. La aplicación ya mostraba `No encontramos el servidor. Revisa tu conexión y la dirección configurada.`. Se actualizó la expectativa y se repitieron tanto el caso aislado como la ronda completa, ambos sin fallos.

## Límites que permanecen abiertos

- El APK comprobado es de desarrollo; falta una identidad de firma release recuperable.
- Firebase Android y la cuenta FCM del servidor no están configurados, por lo que no se puede acreditar push con la app abierta, en segundo plano o cerrada.
- No se capturó ni almacenó foto, video o audio personal. La grabación y el envío real deben repetirse con material sintético autorizado cuando exista el entorno HTTPS final.
- El entorno H2 de demostración no contiene contactos autorizados, por lo que el envío entre dos cuentas debe repetirse contra la base final.
- IA real, moderación del proveedor y transcripción continúan bloqueadas por cuota/configuración externa.
- TalkBack y aceptación visual requieren una persona que escuche y confirme el recorrido; la prueba de semántica y fuente grande no sustituye esa aceptación.

Esta ronda acredita instalación y funcionamiento en un teléfono físico. No acredita todavía APK release, servicios externos ni aceptación académica.
