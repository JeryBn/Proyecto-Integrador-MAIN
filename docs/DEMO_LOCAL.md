# Demostración local de MAIN

Usa una copia limpia de `main` y una base de datos nueva para la demostración. En Windows se necesita JDK 21, Maven, Node.js con npm, Python, MySQL 8.4, Ollama, FFmpeg y Android Studio con el SDK 36, ADB y un emulador. La ruta se verificó desde un checkout nuevo en Windows con Python 3.14, una base MySQL vacía y un emulador existente.

## Compilar

Desde la raíz del repositorio:

```powershell
npm --prefix frontend ci
npm --prefix frontend run build
mvn clean package
python -m venv admin-django/.venv
& admin-django/.venv/Scripts/python.exe -m pip install -r admin-django/requirements.txt
ollama pull gemma3:4b
```

Abre Ollama si el servicio local aún no está escuchando en el puerto 11434. Comprueba que `ollama list` incluya el modelo. La primera respuesta puede tardar mientras se carga en memoria.

## Crear la base de demostración

Con un servidor MySQL local iniciado, usa el cliente MySQL con una cuenta administradora y ejecuta lo siguiente sobre una base nueva. Sustituye los tres marcadores por contraseñas distintas que guardes fuera del repositorio. Los permisos se limitan al esquema de demostración.

```sql
CREATE DATABASE main_demo CHARACTER SET utf8mb4;
CREATE USER 'main_migration'@'localhost' IDENTIFIED BY '<clave de migraciones>';
CREATE USER 'main_spring'@'localhost' IDENTIFIED BY '<clave Spring>';
CREATE USER 'main_django'@'localhost' IDENTIFIED BY '<clave Django>';
GRANT ALL PRIVILEGES ON main_demo.* TO 'main_migration'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON main_demo.* TO 'main_spring'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON main_demo.* TO 'main_django'@'localhost';
```

Los comandos siguientes presuponen MySQL en 3306. Si usas otro puerto, cámbialo tanto en la URL de Spring como en `MAIN_DB_PORT` para Django. Los ejemplos de PowerShell solicitan las contraseñas sin mostrarlas en pantalla.

### API y portal de usuarios

En una terminal en la raíz:

```powershell
$env:MAIN_DB_URL = 'jdbc:mysql://127.0.0.1:3306/main_demo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima&sessionVariables=time_zone=%27-05:00%27'
$env:MAIN_DB_USER = 'main_spring'
$env:MAIN_DB_PASSWORD = Read-Host -MaskInput 'Clave Spring'
$env:SPRING_FLYWAY_USER = 'main_migration'
$env:SPRING_FLYWAY_PASSWORD = Read-Host -MaskInput 'Clave de migraciones'
$env:MAIN_ADMIN_USERNAME = 'admin_demo'
$env:MAIN_ADMIN_PASSWORD = Read-Host -MaskInput 'Nueva clave de administración de MAIN'
$env:MAIN_OLLAMA_MODEL = 'gemma3:4b'
$env:MAIN_CHAT_MODERATION_MODEL = 'gemma3:4b'
$env:MAIN_MEDIA_FFMPEG_DIRECTORY = Split-Path (Get-Command ffmpeg).Source
java -jar target/main-sprint1-0.1.0.jar --spring.profiles.active=mysql --main.demo.enabled=true --main.admin.legacy-enabled=false --server.address=127.0.0.1 --server.port=8088
```

Espera a que Flyway termine y abre el [portal de usuarios](http://127.0.0.1:8088/login.html). Spring crea la cuenta de administración al inicializar la base. Los ejemplos usan `Read-Host -MaskInput` de PowerShell 7.

Para invitar a una cuenta escolar habilitada, genera un código en **Cuentas y vínculos** del panel administrativo. Compártelo directamente con la persona titular. En **Activar cuenta con un código**, el usuario establece su contraseña para ingresar desde la web o desde Android. El código vence en 30 minutos y se invalida al usarlo o generar uno nuevo.

### Panel administrativo

En otra terminal, desde la raíz. Ejecuta las migraciones de Django la primera vez que prepares esa base.

```powershell
Set-Location admin-django
$env:MAIN_DB_NAME = 'main_demo'
$env:MAIN_DB_HOST = '127.0.0.1'
$env:MAIN_DB_PORT = '3306'
$env:MAIN_DB_USER = 'main_migration'
$env:MAIN_DB_PASSWORD = Read-Host -MaskInput 'Clave de migraciones'
$env:MAIN_DJANGO_SECRET_KEY = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
& ./.venv/Scripts/python.exe manage.py migrate --noinput
$env:MAIN_DB_USER = 'main_django'
$env:MAIN_DB_PASSWORD = Read-Host -MaskInput 'Clave Django'
& ./.venv/Scripts/python.exe -m waitress --listen=127.0.0.1:8090 --threads=4 main_admin.wsgi:application
```

Entra al [panel administrativo](http://127.0.0.1:8090/admin.html) con `admin_demo` y la clave que elegiste al iniciar Spring. Para una instalación persistente, guarda la clave de firma de Django fuera de Git y reutilízala al reiniciar.

### Emulador

Abre `mobile` como proyecto de Android Studio, configura JDK 21 y arranca el emulador. Desde la raíz:

```powershell
Set-Location mobile
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
& .\INSTALAR-APP.ps1
```

El script instala la compilación y conecta el puerto 8088 del emulador con la API local. Si hay varios dispositivos, indica `-Device emulator-5554` o el identificador devuelto por `adb devices`.

## Recorrido de la sustentación

- Entra como `docente`, registra una nota y pasa lista en una sesión. Entra como `alumno` para revisar sus cursos, notas y asistencia.
- Desde administración, muestra cuentas, matrículas y estructura escolar. Usa el vínculo autorizado de una cuenta familiar de prueba para demostrar su acceso limitado.
- Entre `alumno` y `otro`, solicita el contacto con el código de seis dígitos o QR, acepta desde la otra cuenta y envía un mensaje. Muestra también estados y publicaciones del campus.
- En el asistente académico, consulta a Gemma con una pregunta corta. Para mostrar una llamada Android a Android, usa dos instancias del emulador con cuentas distintas y establece el puente ADB en ambas.

Las cuentas de demostración `alumno`, `otro`, `vacio` y `docente` usan la contraseña sintética `MainDemo2026!`. El chat de la entrega usa moderación automática en el servidor. Las llamadas no pasan por la IA.
