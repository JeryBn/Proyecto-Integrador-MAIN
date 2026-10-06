# MAIN completo para ejecutar localmente

## Descargar y abrir
1. En el repositorio abre `entregables/MAIN_LOCAL_PAMELA.zip` y pulsa **Download raw file**. El ZIP automatico de Code > Download ZIP contiene el codigo y este paquete dentro de entregables; el paquete ejecutable es MAIN_LOCAL_PAMELA.zip.
2. Extrae todo el archivo, por ejemplo en `Documentos/MAIN_LOCAL`. No ejecutes archivos dentro del ZIP.
3. Necesitas Java JDK 21 o superior instalado. Puedes obtener JDK 21 desde https://adoptium.net/temurin/releases/?version=21 . Comprueba `java -version` o configura JAVA_HOME al JDK. Para ejecutar este paquete no necesitas Maven, Node ni MySQL.
4. Abre `INICIAR-MAIN.cmd` con doble clic. Espera el mensaje `Started MainApplication` y deja la ventana abierta.
5. Abre http://127.0.0.1:8088/login.html . Usuario `alumno`, contraseña `MainDemo2026!`. Tambien existen `otro` y `vacio`, con la misma contraseña.

La aplicacion incluye React compilado, API Spring Boot y datos sinteticos H2 en memoria. Al reiniciar se restablecen los datos. La conexion persistente MySQL/MariaDB es opcional y se documenta en README.md. Esta es la base de demostracion HU06/HU07, no el proyecto final con Django/Kotlin/IA.

## Trabajar en la interfaz
El ZIP incluye el codigo completo. Para entregar cambios, clona el repositorio y usa la rama remota existente `pamela/interfaz-criterios` (`git switch --track origin/pamela/interfaz-criterios` si aun no existe localmente). Copia tus cambios a ese clon, revisa el diff y abre PR.

Abre el proyecto en IntelliJ con JDK 21. Para modificar React instala Node.js LTS y Maven, o usa Maven de IntelliJ. Desde `frontend`: `npm ci` y `npm run build`. Desde la raiz: `mvn clean package`. Copia `target/main-sprint1-0.1.0.jar` a la raiz reemplazando el JAR anterior y reinicia el iniciador. Sin recompilar el JAR seguiras viendo la interfaz anterior. No abras index.html con doble clic; usa la URL del servidor.

`frontend/src/main.jsx` contiene App, ActivityCard y ActivityDetail. `frontend/src/app.css` contiene los estilos. `build.mjs` genera index.html y copia los assets al backend. Login y logout usan sesion y CSRF; no desactives esa proteccion. Prueba listado, detalle, busqueda sin resultados, filtros y cuenta vacio; registra cambios y evidencia reales.

## Si no arranca
- `java no se reconoce`: instala JDK 21 y revisa PATH/JAVA_HOME; vuelve a abrir la ventana.
- `UnsupportedClassVersionError`: estas usando Java anterior a 21.
- `Port 8088 was already in use`: detén la otra instancia o ejecuta `INICIAR-MAIN.cmd 8089` y abre http://127.0.0.1:8089/login.html .
- Falta JAR: extrae el paquete completo, no solo los archivos del frontend.
- No conecta: espera Started MainApplication y comprueba que la ventana del servidor siga abierta.
- Pantalla antigua: compila frontend, empaqueta backend, reemplaza JAR y reinicia; recarga el navegador.

No incluye node_modules, target, credenciales personales ni configuracion de esta PC. La base inicial fue preparada con asistencia registrada en PROMPTS.md; Pamela registra sus modificaciones propias.
