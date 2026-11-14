# Despliegue gratuito en Oracle Cloud

Esta carpeta prepara MAIN para una sola VM **Always Free** de Oracle Cloud. La topología objetivo usa `VM.Standard.A1.Flex` con 2 OCPU, 12 GB de RAM y Ubuntu ARM64. Caddy es el único servicio público; Spring, Django y MySQL no publican puertos. MySQL exige TLS y cada proceso usa una cuenta SQL distinta.

## Límites que no deben cambiarse

- Crear la VM únicamente con la etiqueta **Always Free eligible**, en la región principal de la cuenta.
- Usar como máximo 2 OCPU y 12 GB de memoria para esta VM.
- Usar un volumen de arranque de 50 GB; no añadir recursos de pago.
- Reglas de entrada: TCP 80 y 443 desde Internet; TCP 22 únicamente desde la IP del administrador. No abrir 3306, 8088 ni 8090.
- Conservar una sola IP pública. Confirmar en la pantalla final de Oracle que el costo estimado sea cero antes de crearla.

Oracle documenta que las instancias Always Free se crean solo en la región principal, que la capacidad puede agotarse temporalmente y que las instancias ociosas pueden ser recuperadas. No se genera carga artificial para evitar esa política.

## 1. Crear la VM

En Oracle Cloud selecciona:

1. Imagen Ubuntu 24.04 LTS ARM64.
2. Forma `VM.Standard.A1.Flex`, exactamente 2 OCPU y 12 GB.
3. Volumen de arranque de 50 GB.
4. IP pública y clave SSH propia.
5. Pega `cloud-init.yaml` en **Initialization script** si la consola ofrece ese campo.

Santiago (`sa-santiago-1`) y Bogotá (`sa-bogota-1`) son regiones cercanas a Perú. La región principal queda fijada al crear la cuenta; prioriza la región que muestre capacidad Always Free.

## 2. Dominio sin costo

Se necesita un hostname público que apunte a la IP de la VM. La opción recomendada sin costo es un subdominio de [DuckDNS](https://www.duckdns.org/), por ejemplo `main-jery.duckdns.org`. Configura una sola vez la IP pública reservada de OCI; el token de DuckDNS no entra al repositorio ni al servidor.

Para un ensayo inmediato puede usarse `IP-CON-GUIONES.sslip.io`, por ejemplo `203-0-113-10.sslip.io`, que resuelve sin registro. No es la opción final preferida porque depende de un dominio compartido y la emisión pública de certificados puede alcanzar límites comunes. El dominio debe resolver antes de iniciar Caddy; no se acepta una IP ni `example.org`. Caddy obtiene y renueva el certificado HTTPS automáticamente.

## 3. Preparar y desplegar

Después de que termine `cloud-init`, vuelve a abrir la sesión SSH para actualizar la pertenencia al grupo Docker y ejecuta:

```bash
cd /opt/main/deploy/oci
./prepare.sh --domain main.tu-subdominio.example --admin-username jery.admin --admin-name 'Jery Becerra Ninaquispe'
./deploy.sh
```

`prepare.sh` es idempotente: genera contraseñas aleatorias, una CA privada de MySQL, el certificado del servidor y la clave de cifrado push. Rechaza juegos de secretos incompletos y nunca sobrescribe una clave SQL existente. El acceso inicial queda con permisos `0600` en `initial-access.txt`; el administrador debe cambiar esa contraseña en su primer ingreso.

La carpeta `secrets/` usa modo `0700`. Los archivos que Docker monta se dejan legibles dentro de esa carpeta porque Compose utiliza bind mounts y no remapea el UID para secretos basados en archivos; ningún otro usuario del host puede atravesar el directorio. La clave de la CA, que no entra a ningún contenedor, conserva modo `0600`.

## 4. Verificar

```bash
docker compose ps
curl --fail https://TU_DOMINIO/actuator/health
curl --fail https://TU_DOMINIO/api/admin/health
docker compose exec --no-TTY django /app/entrypoint.sh verify
./backup.sh
./verify-backup.sh backups/main-FECHA.sql.gz
```

El comando `verify` reutiliza la configuración privada del contenedor para comprobar que Django se conecta a MySQL mediante TLS y que el administrador inicial conserva el estado seguro esperado. Solo devuelve un resumen sin contraseñas, certificados ni detalles internos de conexión.

El temporizador `main-backup.timer` conserva respaldos comprimidos con manifiesto SHA-256 durante 14 días. Para activarlo después del primer despliegue:

```bash
sudo systemctl enable --now main-backup.timer
systemctl list-timers main-backup.timer
```

La restauración es deliberadamente manual porque reemplaza datos. Antes de restaurar, detener Spring y Django, crear una base temporal, importar allí el respaldo, ejecutar las comprobaciones de integridad y recién entonces programar la sustitución de producción.

## 5. Integraciones opcionales

- IA: escribir la clave únicamente en `secrets/openai_api_key` y reiniciar Spring/Django. La cuota real debe comprobarse por separado.
- FCM: reemplazar `secrets/firebase-credentials.json`, montar `MAIN_FCM_CREDENTIALS_FILE=/run/secrets/firebase_credentials` y cambiar `MAIN_PUSH_ENABLED=true` en `.env`.
- Moderación multimedia: habilitar solo cuando exista una política aprobada y un revisor autorizado.
- Retención: la cuenta SQL de mínimo privilegio y el perfil `retention` quedan preparados, pero el trabajador no se activa sin aprobación de política y custodia. Después de aprobarlas, configura el valor de autorización documentado por el proyecto en `MAIN_RETENTION_APPLY` y ejecuta `docker compose --profile retention up -d retention`.

Los archivos `.env`, `secrets/`, `backups/` e `initial-access.txt` permanecen fuera de Git. No copiar sus contenidos a Trello, evidencias o capturas.

## Actualizaciones y reversión

Antes de actualizar, ejecutar `backup.sh`. Luego traer un commit revisado, construir y desplegar:

```bash
git fetch origin
git switch main
git pull --ff-only
./deploy.sh
```

Las imágenes se reconstruyen y los contenedores se reemplazan; los volúmenes de MySQL y Caddy permanecen. Para volver atrás, seleccionar un commit anterior conocido, ejecutar `deploy.sh` y no restaurar la base salvo que una migración irreversible lo exija.

## Referencias oficiales

- Oracle Always Free: https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm
- Regiones OCI: https://docs.oracle.com/en-us/iaas/Content/General/Concepts/regions.htm
- Docker en Ubuntu/ARM64: https://docs.docker.com/engine/install/ubuntu/
- HTTPS automático de Caddy: https://caddyserver.com/docs/automatic-https
