# Preparación de Oracle Cloud Always Free — 13 de noviembre de 2026

Jery eligió Oracle Cloud Always Free para evitar un servidor con costo recurrente. Este incremento deja el proyecto preparado para una sola VM ARM64; no crea una cuenta, una máquina ni un cargo y no afirma todavía disponibilidad pública.

## Arquitectura preparada

- VM objetivo `VM.Standard.A1.Flex`: 2 OCPU, 12 GB de RAM y Ubuntu ARM64, dentro del límite Always Free documentado por Oracle.
- Caddy es el único proceso publicado en TCP 80/443. MySQL, Spring 8088 y Django 8090 permanecen sin puertos del host y comparten una red interna de contenedores.
- MySQL 8.4 exige TLS 1.2/1.3. `prepare.sh` crea una CA privada y un certificado cuyo SAN es `mysql`; Spring importa esa CA a un truststore temporal y Django usa `VERIFY_IDENTITY`.
- Cuentas separadas `main_migration`, `main_spring`, `main_django`, `main_retention` y `main_operator`. El proceso web Django recibe permisos por tabla; el trabajador de retención queda en un perfil desactivado hasta aprobar la política.
- Los secretos se entregan como archivos montados, no como argumentos de construcción ni archivos versionados. La preparación es idempotente y rechaza claves SQL o juegos TLS incompletos.
- El administrador inicial se crea con un comando Django transaccional. Una segunda ejecución no cambia la contraseña, el rol ni el nombre de una identidad existente.
- Los servicios persistentes tienen límites de recursos, comprobaciones de salud, reinicio automático, sistema de archivos de solo lectura cuando corresponde y rotación de logs.
- Respaldo MySQL diario comprimido, verificación `gzip`, manifiesto SHA-256 y conservación de 14 días. La restauración permanece manual para impedir reemplazos accidentales.

## Automatización y manejo de errores

`deploy/oci/cloud-init.yaml` prepara el host; `bootstrap-host.sh` instala Docker desde su repositorio oficial y no cambia UFW, de acuerdo con la advertencia de Oracle para sus imágenes. `prepare.sh` valida dominio, identidad y archivos privados antes de generar configuración. `deploy.sh` espera la salud de todos los servicios y, ante fallo, muestra estado y logs acotados. Ningún script elimina volúmenes de producción automáticamente.

La integración continua valida sintaxis, composición, construcción de imágenes, migraciones Flyway/Django, TLS MySQL y provisión del administrador usando datos sintéticos y volúmenes desechables.

## Verificación local de este corte

- Spring Boot: 158 pruebas, cero fallos.
- Django: 91 pruebas, cero fallos; se añadieron la provisión idempotente del primer ADMIN y TLS verificado también cuando se proporciona CA para una base local.
- Web: 42 pruebas, auditoría npm sin vulnerabilidades conocidas y compilación reproducible.
- Contrato OCI: validador PowerShell aprobado, YAML válido y todos los scripts Bash superan análisis sintáctico.

## Acciones externas que permanecen

1. Iniciar sesión o crear la cuenta Oracle, escoger conscientemente la región principal y confirmar en pantalla `Always Free eligible` y costo cero.
2. Crear la VM, reservar/asignar su IP pública y abrir únicamente 80/443 más SSH restringido a la IP de Jery.
3. Registrar un subdominio gratuito DuckDNS, apuntarlo a esa IP y ejecutar el despliegue.
4. Repetir Postman y conectar Android al origen HTTPS real.
5. Incorporar por canales privados Firebase/FCM y OpenAI; la infraestructura ya dispone de puntos de montaje, pero no inventa esas credenciales.
6. Ejecutar respaldo y restauración de ensayo en el servidor, además de aceptación humana y teléfono físico.

Guía operativa: `deploy/oci/README.md`. Los datos de acceso, tokens, certificados privados y contraseñas nunca deben copiarse a Trello ni a evidencias.
