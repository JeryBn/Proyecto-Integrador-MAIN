# Carga local sostenida al 30 de octubre

Avance M15 independiente de un servidor HTTPS público. Se amplió verificar_carga_local.py con duración de lectura, pausa mínima para tandas sostenidas y mezcla de endpoints, conservando el modo anterior por cantidad fija. Solo admite HTTP en loopback, entre uno y dieciséis trabajadores, máximo mil lecturas por trabajador y duración sostenida entre treinta y seiscientos segundos. Una tanda sostenida exige una pausa de al menos cien milisegundos. No es un generador ilimitado de estrés.

## Resultado de la ejecución

Ocho sesiones sintéticas autenticadas ejecutaron lecturas durante al menos 120 segundos cada una, con pausa de 250 ms entre solicitudes. Spring usó MySQL local aislado en el puerto 3307. El tiempo total, incluido acceso y salida de las sesiones, fue 120.61 segundos. Se obtuvieron 3666 respuestas HTTP 200 válidas y cero errores.

| Endpoint | Solicitudes | Errores |
|---|---:|---:|
| Actividades pendientes | 1224 | 0 |
| Perfil de la cuenta autenticada | 1219 | 0 |
| Buzón de avisos | 1223 | 0 |
| Total | 3666 | 0 |

Mediana de lectura 13.26 ms, percentil 95 de 16.86 ms y máximo de 25.88 ms. Estas latencias excluyen el acceso, cierre de sesión y pausas; no representan el tiempo de una pantalla ni una red de Internet. El límite deliberado de frecuencia y el uso de una sola PC impiden presentar esta ronda como capacidad máxima de usuarios o certificación de producción.

Se comprobó que cada perfil correspondiera a la identidad sintética esperada; los otros dos endpoints debían devolver listas JSON válidas. No se guardaron contenidos de actividades, avisos, cookies, tokens o claves en el informe. Las ocho sesiones se cerraron al terminar. No se modificaron preferencias, cursos o datos de personas reales ni se invocó IA o FCM.

El modo anterior por cantidad fija también se ejecutó con dos trabajadores y cinco lecturas cada uno: diez respuestas correctas, sin errores. Es una comprobación de compatibilidad del verificador y no se suma a las 3666 solicitudes sostenidas.

## Reproducción

Desde la raíz del repositorio, con el servidor sintético local disponible:

    python scripts/verificar_carga_local.py resultado-local.json --workers 8 --reads 1000 --seconds 120 --interval-ms 250 --mixed --base http://127.0.0.1:8088 --environment "Describir la base y procesos realmente utilizados"

La cuenta alumno y su contraseña de demostración pertenecen exclusivamente a este entorno sintético. No sustituirlas por credenciales institucionales ni publicar volcados de respuestas. Evidencia saneada de esta ronda: docs/informes/2026-10-30-carga/sustained.json.

La paginación y privacidad de mensajes se comprobaron por separado con 25 condiciones HTTP/MySQL, 35 mensajes sintéticos, 33 aprobados y retirada del receptor. Se documentan en PAGINACION_MYSQL_30_OCT.md; no se suman como solicitudes de carga.

## Pendientes de M15

Siguen pendientes carga y disponibilidad del destino definitivo, pruebas de red y dispositivos identificados, FCM real, disponibilidad del asistente y aceptación independiente del equipo. El error de persistencia reportado por Pamela no se reprodujo en la regresión local anterior; no se declara corregido en su PC sin sus logs y una repetición allí. La evidencia Android anterior que Jery indicó que conserva su compañero debe enlazarse, sin inferir que corresponde a un teléfono físico.
