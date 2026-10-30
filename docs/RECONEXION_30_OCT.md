# Reconexión local de chats al 30 de octubre

Avance M06. Se ejecutaron el planificador de lecturas y el cliente HTTP JavaScript reales frente a Spring, Django y MySQL locales. Un proxy de prueba limitado a GET cortó sus conexiones durante 75 segundos reales. No se interrumpieron los servidores ni la conexión de Windows. Durante el corte se envió y aprobó otro mensaje sintético desde una sesión distinta.

Resultado: diez condiciones JavaScript y dieciséis comprobaciones HTTP aprobadas. Cuatro lecturas fallidas; espera máxima de reintento 60 segundos; una sola lectura simultánea. La recuperación por el mismo mecanismo de despertar del cliente tardó 34 ms en esta ronda local, recuperó los dos mensajes sin duplicados y volvió al intervalo de cinco segundos. Detener el planificador impidió consultas posteriores. Los reintentos de envío devolvieron el mismo identificador; el servidor conservó dos mensajes y dos avisos. Ambas cuentas sintéticas se deshabilitaron.

El primer ensayo duró 65 segundos y falló una expectativa del verificador: el calendario 10 + 20 + 40 alcanza la siguiente lectura a los 70 segundos. Se amplió a 75 para observar el límite real de 60 segundos. No se modificó el cliente ni se presenta ese intento fallido como aprobado.

Reproducción explícita: iniciar los servidores locales aislados; configurar MAIN_TEST_ADMIN_PASSWORD en el entorno privado y ejecutar scripts/verificar_reconexion_chat.py. El proceso Node recibe la sesión exclusivamente por entrada estándar; el resultado público no incluye cookies, claves ni mensajes. Evidencia: docs/informes/2026-10-30-reconexion/results.json.

Esto prueba recuperación web ante cortes inducidos y servidor real local. No acredita red móvil física, entrega FCM con la app cerrada ni aceptación de la matriz de roles por el equipo. Los seis pares de roles y sus dos sentidos ya están cubiertos por las pruebas Spring incorporadas en 4d42dfb; la nueva regresión del 30/10 aprobó 149 pruebas Spring. M06 conserva esas condiciones de aceptación y las dependencias de M10.
