# Administración Django de MAIN

Implementación asistida para Jery Becerra Ninaquispe, según la arquitectura que volvió a proporcionar el 21/10/2026. Conserva el React aportado por Pamela Regina Meneses Mayhua y el contrato administrativo previo de Michael Julian Fernandez Camarena. El aporte Spring Boot anterior permanece disponible para compatibilidad; no se reescribe su autoría.

## Alcance

React → Django → MySQL compartido ← Spring Boot ← web de alumnos y Kotlin.

Django implementa ingreso ADMIN, perfil, salida, actividades, cuentas, vínculos, contactos/reportes, cursos/matrículas, horarios, notas administrativas, auditoría y moderación de mensajes/estados con descarga autorizada de adjuntos. Mantiene los contratos React bajo `/api/admin` y `/api/admin/v1`. Lee/escribe directamente la base compartida.

La interfaz React servida por Django habilita actividades, comunidad, cursos/matrículas y revisión. Horarios y notas administrativas conservan su API; la app docente y estudiantil consulta Spring. La paridad global de toda la web con Android sigue siendo otra entrega M21. [Contrato y verificaciones del 22 de octubre](../docs/MIGRACION_DJANGO_22_OCT.md).

## Preparación

Python 3.12, dependencias fijadas en `requirements.txt` y MySQL compatible con Django 5.2. Crear un entorno virtual, instalar las dependencias y compilar React desde `frontend` con `npm ci` y `node build.mjs`.

Variables obligatorias: `MAIN_DJANGO_SECRET_KEY`, `MAIN_DB_NAME`, `MAIN_DB_USER`, `MAIN_DB_PASSWORD`. Opcionales: `MAIN_DB_HOST` (127.0.0.1), `MAIN_DB_PORT` (3306), `MAIN_DJANGO_HOSTS` (127.0.0.1,localhost), `MAIN_HTTPS` (false para pruebas locales). No guardar credenciales en Git.

Primero iniciar Spring con perfil `mysql` para aplicar Flyway V1–V11. Después ejecutar `python manage.py migrate --noinput` usando una cuenta de migraciones: Django solo añade `django_session` y su registro `django_migrations`. Ejecutar con una cuenta limitada por tabla, según [el contrato de permisos](../docs/MIGRACION_DJANGO_22_OCT.md). No conceder DDL ni permisos globales al proceso web. El trabajador de moderación necesita SELECT/INSERT/UPDATE en `automatic_assessment` (V11); [configuración, política y límites del incremento del 25/10](../docs/PUSH_Y_MODERACION_25_OCT.md). Push usa las tablas V10 exclusivamente desde Spring.

Arranque local: `python -m waitress --listen=127.0.0.1:8090 --threads=4 main_admin.wsgi:application`. Las cuentas ADMIN deben existir en `main_user`, creadas por el arranque configurado de Spring; Django no crea una identidad paralela ni eleva alumnos. Las sesiones son independientes de Spring y revalidan rol, activación y versión de contraseña.

Para reabrir el entorno ya preparado en Windows, `scripts/Start-SharedArchitecture.ps1` recibe rutas explícitas al JSON privado, Java, Python, MySQL, directorio de datos y JAR. El JSON contiene `database`, `port`, `spring_password`, `migration_password`, `django_password` y `django_secret`; debe quedar fuera de Git. El iniciador no crea bases ni modifica contraseñas y rechaza un puerto ocupado por otro proceso. La opción `-DemoData` habilita únicamente identidades sintéticas. Se comprobó su análisis y reutilización de los tres procesos ya abiertos; el inicio desde una PC sin el entorno preparado no está acreditado.

Pasar también `-FfmpegDirectory` con la carpeta existente de FFmpeg para conservar el procesamiento de audio/video de Spring al reabrir. El iniciador comprueba el ejecutable y transmite la ruta por entorno; no descarga herramientas ni inserta secretos en argumentos.

## Contrato compartido

- Flyway es dueño de las tablas de negocio; modelos Django `managed=False`. No ejecutar generación destructiva de esquema ni recrear tablas compartidas.
- Identificadores BIGINT positivos hasta 9007199254740991; no convertir a float. Claves de actividad manuales, duplicado devuelve 409 sin sobrescribir.
- `fechaLimite` usa `YYYY-MM-DDTHH:mm:ss`, hora de Lima sin offset. Sesión SQL en −05:00. El TIMESTAMP MySQL de este esquema limita altas Django a años 1970–2037.
- Contraseñas BCrypt compatibles con Spring; sesión en DB con rotación, vencimiento de 30 minutos, CSRF también en login/logout y cookies HttpOnly/SameSite. Límite de ingreso local a cada proceso; antes de escalar usar contador compartido/proxy.
- Una creación y su auditoría se confirman en la misma transacción. La auditoría guarda actor, acción, ID y fecha; no copia contraseña, título ni descripción.
- Producción requiere HTTPS/proxy, cookies seguras, credenciales propias y validación de despliegue; el entorno entregado solo escucha loopback.

## Verificación reproducible

`python manage.py test administration --settings=main_admin.test_settings --noinput -v 2`: 32 casos en SQLite desechable, sin tocar MySQL compartido.

El iniciador establece `MAIN_ADMIN_LEGACY_ENABLED=false`: las antiguas rutas administrativas de Spring quedan cerradas (410; una escritura sin CSRF puede ser rechazada antes con 403). La compatibilidad histórica de Java se conserva bajo `main.admin.legacy-enabled=true` para pruebas o transición explícita; no usarla en el despliegue que exige administración exclusivamente Django. El filtro Spring verifica rol, habilitación y huella de contraseña compartidos en cada petición autenticada.

`scripts/verificar_arquitectura_compartida.py` prueba por HTTP los dos backends reales sobre MySQL. Configurar `MAIN_TEST_ADMIN_PASSWORD`, `MAIN_TEST_STUDENT_PASSWORD` y cuentas sintéticas. Modo `create`: 19 comprobaciones y una actividad nueva; modo `verify`: 16 comprobaciones de la misma actividad después del reinicio. El JSON de evidencia no contiene secretos. No ejecutar sobre datos reales.

La prueba Android `SharedArchitectureUiTest` recibe `sharedActivityId` como argumento y consulta el registro creado por Django a través de Spring. El ID debe corresponder al ensayo sintético, no a una actividad de producción.

Referencias: [modelos sobre tablas existentes](https://docs.djangoproject.com/en/5.2/howto/legacy-databases/), [CSRF](https://docs.djangoproject.com/en/5.2/howto/csrf/), [bases compatibles](https://docs.djangoproject.com/en/5.2/ref/databases/).
