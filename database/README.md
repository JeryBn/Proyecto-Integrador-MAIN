# Base de datos COLEGIA

Compose crea una base MySQL 8.0 nueva y conserva sus datos en el volumen mysql-data del proyecto. No conecta con la MariaDB local anterior ni borra sus archivos. Flyway, desde Spring, es la única fuente de creación/evolución del esquema de negocio. Django usa managed=False y migra únicamente sus tablas internas.

`seed.sql` contiene exclusivamente datos académicos ficticios; se carga después de Flyway mediante `python manage.py seed_colegia` desde Django. El comando también crea hashes BCrypt de cuentas demo y un vínculo familiar; rechaza una base con alumnos. Nunca montarlo en docker-entrypoint-initdb.d ni ejecutarlo sobre una base con datos reales.

`01_CREAR_BASE.sql` y cualquier script.sql anterior son archivos históricos de inicialización; no forman parte del primer arranque Compose y no compiten con Flyway. No reimportarlos en el esquema actual.

MySQL 8 debe validarse desde volumen nuevo aislado. Las migraciones existentes no se alteraron: no hay una ejecución Docker local que justifique cambiar sus checksums. La compatibilidad definitiva y persistencia tras reinicio están pendientes si no hay motor Docker; consultar docs/verificacion-colegia.md. Diferencias futuras deben resolverse mediante una migración nueva que preserve datos y aplicando reparación de historial solo con diagnóstico y respaldo.

La cuenta de desarrollo Compose dispone de permisos sobre su base para permitir Flyway y las migraciones internas. En producción separar credenciales de migración y de ejecución usando SPRING_FLYWAY_USER/PASSWORD y permisos mínimos para Django/Spring, como en el despliegue OCI histórico.
