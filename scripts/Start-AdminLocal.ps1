param(
  [string]$LocalWorkRoot = (Join-Path ([Environment]::GetFolderPath('MyDocuments')) 'Codex\2026-10-22\ho\work'),
  [string]$PythonExecutable = (Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'),
  [int]$Port = 8000
)
$ErrorActionPreference = 'Stop'
$repository = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$application = Join-Path $repository 'backend\django-admin'
$privateFile = Join-Path $LocalWorkRoot 'main-review-private.json'
foreach ($file in @($privateFile, $PythonExecutable)) {
  if (!(Test-Path -LiteralPath $file -PathType Leaf)) { throw "No existe $file. Configura LocalWorkRoot o PythonExecutable para este equipo." }
}
$config = Get-Content -LiteralPath $privateFile -Raw | ConvertFrom-Json
foreach ($name in @('database', 'port', 'django_password', 'django_secret')) {
  if (!$config.$name) { throw "Falta $name en la configuracion privada." }
}
function Test-LocalPort([int]$Number) {
  $client = [Net.Sockets.TcpClient]::new()
  try { return $client.ConnectAsync('127.0.0.1', $Number).Wait(750) -and $client.Connected }
  catch { return $false }
  finally { $client.Dispose() }
}
if (Test-LocalPort $Port) { throw "El puerto $Port ya esta ocupado. No se detiene ningun proceso. Comprueba http://localhost:5174/admin.html." }
$libraries = Join-Path $LocalWorkRoot 'main-python-libs'
if (!(Test-Path -LiteralPath $libraries -PathType Container)) { throw "Faltan las dependencias Python en $libraries." }
$env:PYTHONPATH = "$application;$libraries"
$env:PYTHONDONTWRITEBYTECODE = '1'
$env:MAIN_DJANGO_SECRET_KEY = $config.django_secret
$env:MAIN_DB_HOST = '127.0.0.1'
$env:MAIN_DB_PORT = [string]$config.port
$env:MAIN_DB_NAME = $config.database
$env:MAIN_DB_USER = 'main_django'
$env:MAIN_DB_PASSWORD = $config.django_password
$env:COLEGIA_ADMIN_PORT = [string]$Port
$env:DJANGO_SETTINGS_MODULE = 'main_admin.settings'
if (!(Test-LocalPort ([int]$config.port))) {
  $server = Join-Path $LocalWorkRoot 'main-db-runtime\mariadb-11.4.10-winx64\bin\mariadbd.exe'
  $ini = Join-Path $LocalWorkRoot 'main-pamela-db\my.ini'
  foreach ($file in @($server, $ini)) { if (!(Test-Path -LiteralPath $file -PathType Leaf)) { throw "No existe $file. No se crea ni reemplaza la base." } }
  $logs = Join-Path $env:TEMP 'colegia-admin-local'
  New-Item -ItemType Directory -Force -Path $logs | Out-Null
  $stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
  $databaseProcess = Start-Process -FilePath $server -ArgumentList @("--defaults-file=`"$ini`"", '--bind-address=127.0.0.1', '--console') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logs "db-$stamp.log") -RedirectStandardError (Join-Path $logs "db-$stamp.error.log")
  $deadline = [DateTime]::UtcNow.AddSeconds(30)
  while (!(Test-LocalPort ([int]$config.port))) {
    if ($databaseProcess.HasExited -or [DateTime]::UtcNow -gt $deadline) { throw "MariaDB no inicio. Revisa $logs. No se modifico la configuracion de datos." }
    Start-Sleep -Milliseconds 250
  }
}
Set-Location -LiteralPath $application
$program = @'
import os
import django
django.setup()
from django.db import connection
from django.core.management import call_command
from main_admin.wsgi import application
from waitress import serve
with connection.cursor() as cursor:
    cursor.execute('SELECT 1')
    cursor.fetchone()
call_command('check')
port = int(os.environ['COLEGIA_ADMIN_PORT'])
print(f'Django conectado a la base. API: http://127.0.0.1:{port}', flush=True)
print('Panel: http://localhost:5174/admin.html', flush=True)
print('Deja esta terminal abierta. Ctrl+C detiene Django.', flush=True)
serve(application, host='127.0.0.1', port=port, threads=4)
'@
$program | & $PythonExecutable -
exit $LASTEXITCODE
