# Proyecto Integrador MAIN · Sección A

MAIN reúne el portal académico web, la aplicación Android y la administración institucional. Estudiantes y docentes acceden desde React o Android mediante la API de Spring Boot. El panel React de administración usa Django. Ambos servicios comparten MySQL en el entorno integrado.

## Funciones

- **Estudiante:** consulta cursos, horarios, notas y actividades. Puede guardar borradores y enviar entregas desde la web.
- **Docente:** gestiona actividades y notas en sus cursos autorizados, consulta entregas y guarda la asistencia en web y Android por fecha y sesión.
- **Comunidad:** chats privados y grupos, estados multimedia, avisos, privacidad y revisión previa del contenido.
- **Administración:** gestiona cuentas, matrículas, actividades y estructura escolar. Puede crear años, períodos, grados y secciones y vincular cursos, con auditoría.
- **Asistente académico:** Gemma 3 (4B) a través de Ollama local. Web y Android comparten la API del asistente.

## Ejecutar desde el código de `main`

Con JDK 21, Maven y Node.js instalados, ejecuta desde la raíz del repositorio:

```powershell
npm --prefix frontend ci
npm --prefix frontend run build
mvn clean package
java -jar target/main-sprint1-0.1.0.jar --spring.profiles.active=default --main.demo.enabled=true
```

Abre [el portal de demostración local](http://127.0.0.1:8088/login.html). Este modo usa datos ficticios en memoria. Mantén este entorno separado de cualquier base con datos reales.

Para probar el **sistema integrado**, prepara MySQL y Django según la [guía administrativa](admin-django/README.md) y la [arquitectura compartida](docs/ARQUITECTURA.md). Con los servicios configurados, el panel está en [Administración local](http://127.0.0.1:8090/admin.html). Usa la configuración privada del equipo o crea tus propias credenciales de prueba.

Para activar **Gemma**, ejecuta `ollama pull gemma3:4b` y mantén Ollama escuchando en `127.0.0.1:11434`. En PowerShell, establece `$env:MAIN_OLLAMA_MODEL = 'gemma3:4b'` antes de arrancar Spring. El modelo puede tardar más en la primera consulta. Para activar la moderación privada de chats, establece también `$env:MAIN_CHAT_MODERATION_MODEL = 'gemma3:4b'` antes de arrancar Spring. MAIN analiza chats y estados de texto o imagen con Ollama en el mismo equipo. Si el modelo no responde, conserva el contenido privado y programa un reintento. El audio y video de estados y las publicaciones del campus tienen revisión administrativa.

Para **Android**, consulta la [guía de compilación](mobile/README.md). La variante de entrega incorpora el origen HTTPS indicado mediante `MAIN_PUBLIC_ORIGIN`: el estudiante solo introduce usuario y contraseña. Si se usa un túnel temporal y cambia su URL, recompila el APK. Para firmarlo, sigue la [guía de firma privada](docs/FIRMA_PRIVADA_ANDROID.md).

## Verificación y alcance al 19 de noviembre de 2026

- Pasaron 173 pruebas de Spring, 52 web y 94 de Django en las rondas registradas. Android pasó 13 pruebas unitarias y seis instrumentadas en un Motorola con Android 15.
- Se verificó el acceso de alumno y docente en la web por HTTPS, la actualización del APK firmado, el ingreso en Android con el servidor preconfigurado y una respuesta de Gemma desde la app. [Evidencia Android](docs/informes/2026-11-16-android-fisico/LEEME.md)
- En MySQL local pasaron 19 comprobaciones de integración y 38 de moderación. También se verificó la edición desde Django, su reflejo en Spring y el guardado de una entrega del alumno.
- Queda el ensayo final con el equipo o docente, incluida la consulta de esa entrega por el docente y la comprobación de Android sobre la base compartida. Las notificaciones push con la app cerrada requieren configurar Firebase/FCM y probar la recepción en el teléfono.

La presentación puede usar HTTPS temporal con datos ficticios y la laptop encendida. Antes de distribuir el APK firmado, guarda un respaldo recuperable de la firma. Mantén fuera de Git las contraseñas, credenciales de proveedores, archivos privados de configuración y claves de firma.

## Identidad institucional

Para una instalación dedicada, configura `MAIN_CAMPUS_NAME` y `MAIN_CAMPUS_COLOR` (formato `#RRGGBB`) antes de iniciar Spring. El portal muestra estos valores en su cabecera. Usa el mismo `MAIN_CAMPUS_NAME` al compilar Android para cambiar el nombre visible dentro de la app. Cada instalación debe usar su propia configuración y su propia base. La personalización visual no habilita el alojamiento compartido entre escuelas.

## Equipo e historial

Trabajo integrado en `main` con los aportes de Jery Becerra Ninaquispe, Pamela Regina Meneses Mayhua y Michael Julian Fernandez Camarena. Consulta las [pautas de colaboración](CONTRIBUTING.md) y la [documentación histórica](docs/REVISION_2026_10_20.md).

El [ZIP del primer incremento](entregables/MAIN_LOCAL_PAMELA.zip) corresponde a la entrega histórica HU06/HU07. Para obtener el sistema actual, descarga o clona la rama `main`.


### Llamadas web entre redes

Para habilitar el relevo WebRTC, inicia Spring con `MAIN_TURN_URLS` (URLs `turn:` o `turns:` separadas por comas) y `MAIN_TURN_SECRET` (el mismo secreto que `static-auth-secret` en coturn con `use-auth-secret`). Guarda el secreto en el entorno del servidor. Los clientes autenticados reciben credenciales temporales. Sin estas variables, MAIN intenta establecer una conexión directa. El navegador requiere HTTPS para usar la cámara y el micrófono, salvo en localhost.