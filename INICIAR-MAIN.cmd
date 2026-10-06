@echo off
setlocal
cd /d "%~dp0"
set "MAIN_JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "MAIN_JAVA=%JAVA_HOME%\bin\java.exe"
"%MAIN_JAVA%" -version >nul 2>&1
if errorlevel 1 (
 echo Necesitas Java JDK 21 o superior. Lee LEEME-LOCAL.md.
 pause
 exit /b 1
)
if not exist "main-sprint1-0.1.0.jar" (
 echo Falta el JAR. Descarga y extrae el ZIP completo de entregables.
 pause
 exit /b 1
)
set "MAIN_PORT=8088"
if not "%~1"=="" set "MAIN_PORT=%~1"
echo MAIN: http://127.0.0.1:%MAIN_PORT%/login.html
echo Deja esta ventana abierta. Para detenerlo presiona Ctrl+C.
"%MAIN_JAVA%" -jar "main-sprint1-0.1.0.jar" --server.port=%MAIN_PORT%
echo Si fallo, revisa el mensaje anterior y LEEME-LOCAL.md.
pause
