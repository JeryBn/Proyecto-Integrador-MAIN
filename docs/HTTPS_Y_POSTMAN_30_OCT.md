# Preparación HTTPS y pruebas Postman al 30 de octubre

M16 y M15 avanzan con una configuración de proxy completa y una colección ejecutable. El laboratorio HTTPS está comprobado en esta PC. No hay despliegue público, dominio contratado ni servidor externo verificado.

## Arquitectura de acceso

Caddy recibe HTTPS y conserva un único origen para ambos servicios. /admin.html, /assets/admin.js, /assets/admin.css, /api/admin y sus subrutas y /moderation.html, .js y .css van a Django. Las demás rutas van a Spring. Se corrigió el Caddyfile anterior que enviaba todo a Spring. No se quitan prefijos de las API. Las sesiones MAIN_ADMIN_SESSION y JSESSIONID permanecen independientes.

Los servicios escuchan solo en loopback. Caddy sobrescribe X-Forwarded-Proto y retira Forwarded del cliente. Spring producción usa procesamiento nativo de cabeceras limitado al proxy local, cookies Secure, HttpOnly y SameSite Lax, demo desactivada y administración histórica desactivada. Django usa main_admin.production_settings: exige MAIN_PUBLIC_ORIGIN HTTPS exacto, limita host y origen CSRF y requiere cookies seguras. Waitress debe ejecutarse con --listen=127.0.0.1:8090 --trusted-proxy=127.0.0.1 --trusted-proxy-headers=x-forwarded-proto main_admin.wsgi:application. Django confía en el esquema WSGI ya validado por Waitress, no directamente en una cabecera arbitraria del navegador.

## Laboratorio comprobado

Start-HttpsLab.ps1 inicia copias separadas de Spring 8188 y Django 8190 sobre la base local aislada ya migrada, y Caddy en https://localhost:8443 vinculado exclusivamente a 127.0.0.1. Necesita ConfigurationPath, JavaExecutable, PythonExecutable y CaddyExecutable; no recibe contraseñas como argumentos. Usa el JAR ya compilado. No ejecuta migraciones, crea usuarios demo, instala certificados de confianza, abre firewall ni publica la PC. Rechaza puertos ocupados. Las sesiones de prueba no cambian las de 8088/8090.

La CA y sus claves quedan en var/caddy, ignoradas por Git. Solo el certificado público var/caddy/pki/authorities/local/root.crt se proporciona explícitamente al cliente de pruebas. Una petición sin esa CA falló con UNABLE_TO_GET_ISSUER_CERT_LOCALLY. Newman verificó la cadena y el nombre localhost con TLS activado. El certificado local no se instala en Windows ni constituye certificado público de producción. No abrir excepciones del navegador para usarlo.

Caddy 2.11.7 se descargó del repositorio oficial y el ZIP se verificó contra la suma SHA512 publicada. Las configuraciones pública y local superaron caddy adapt --validate. La configuración pública se validó con main.example.org como marcador, sin iniciar ACME. No se afirma que ese dominio pertenezca al proyecto.

## Pruebas en Postman

Importar postman/MAIN-HTTPS.postman_collection.json y uno de los dos entornos. MAIN-local-HTTPS usa https://localhost:8443; MAIN-servidor-HTTPS contiene un dominio de ejemplo que debe reemplazarse. Los cuatro campos de acceso están vacíos: username y password de una cuenta ALUMNO sintética, adminUsername y adminPassword de un ADMIN de pruebas. Mantener los valores privados locales o en el almacén privado de Postman; no exportar ni sincronizar entornos con secretos.

Activar la verificación SSL. Para el laboratorio añadir la CA pública en Settings > Certificates > CA certificates. No desactivar SSL verification. El servidor futuro con certificado público válido no debe requerir esa CA local. En Postman ejecutar la colección completa en orden y mantener activado su gestor de cookies. Las solicitudes de acceso desactivan seguimiento automático de redirecciones para comprobar el resultado y conservar el recorrido posterior explícito.

La colección hace 25 solicitudes y 55 comprobaciones: páginas de usuario y administración, rechazo anónimo, CSRF de ambos servicios, acceso alumno, identidad, pendientes, detalle inexistente, avisos, rechazo de escritura sin CSRF, salida y revocación, rechazo de alumno en administración, acceso ADMIN, actividades Django, origen ajeno rechazado, cookies Secure/HttpOnly/SameSite y cabeceras HTTPS. No crea cuentas, notas, actividades o mensajes. No prueba IA, FCM, carga ni todos los contratos del proyecto.

Se ejecutó la colección con Newman 6.2.3 sobre los servicios reales del laboratorio: 25 solicitudes, 55 comprobaciones, cero fallos. scripts/run-postman.cjs recibe las credenciales por variables de entorno privadas y solo guarda un resumen saneado. MAIN_TEST_ORIGIN define el origen; MAIN_TEST_USERNAME, MAIN_TEST_USER_PASSWORD, MAIN_TEST_ADMIN_USERNAME y MAIN_TEST_ADMIN_PASSWORD definen las cuentas. MAIN_TEST_CA_CERT indica la CA adicional opcional. Instalar Newman en herramientas externas y exponer su node_modules mediante NODE_PATH. Nunca pasar claves con --env-var en la línea de comandos ni exportar el registro completo de la ejecución.

El primer recorrido exigía que Spring renovara una cookie en una solicitud que ya tenía sesión. Se corrigió esa expectativa para verificar la cookie nueva del acceso; la segunda ronda aprobó todas las comprobaciones. La evidencia final está en docs/informes/2026-10-30-https/postman-results.json y no incluye contraseñas, cookies ni cuerpos de respuesta.

## Pendiente para un servidor real

Elegir servidor y dominio, asignar DNS y presupuesto y habilitar únicamente los puertos públicos necesarios de Caddy. Mantener MySQL y backends privados. Configurar Spring con perfil production y las credenciales SQL correspondientes; su conexión MySQL exige TLS con verificación de identidad. Configurar Django con DJANGO_SETTINGS_MODULE=main_admin.production_settings, MAIN_PUBLIC_ORIGIN y credenciales acotadas. Para MySQL remoto falta configurar y comprobar también su TLS en Django antes de transmitir credenciales por una red externa. Migraciones, respaldo/restauración y operación supervisada deben comprobarse en ese destino.

Después validar el dominio/certificado público, repetir esta colección con cuentas sintéticas, conectar Android a ese origen y documentar el recorrido. Hosting, DNS, disponibilidad pública, integración Android contra ese destino y recepción FCM permanecen pendientes. Preparar y aprobar un laboratorio no cierra M16 ni S3-04.

## Referencias oficiales

https://caddyserver.com/docs/caddyfile/directives/reverse_proxy

https://caddyserver.com/docs/automatic-https

https://docs.pylonsproject.org/projects/waitress/en/stable/reverse-proxy.html

https://docs.spring.io/spring-boot/how-to/webserver.html

https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-cookies
