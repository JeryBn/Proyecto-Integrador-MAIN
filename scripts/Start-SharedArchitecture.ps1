param(
  [Parameter(Mandatory=$true)][string]$ConfigurationPath,
  [Parameter(Mandatory=$true)][string]$JavaExecutable,
  [Parameter(Mandatory=$true)][string]$PythonExecutable,
  [Parameter(Mandatory=$true)][string]$MySqlExecutable,
  [Parameter(Mandatory=$true)][string]$MySqlDataDirectory,
  [string]$JarPath,
  [string]$FfmpegDirectory,
  [switch]$DemoData
)
$ErrorActionPreference='Stop'
$repository=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if(!$JarPath){$JarPath=Join-Path $repository 'target/main-sprint1-0.1.0.jar'}
foreach($file in @($ConfigurationPath,$JavaExecutable,$PythonExecutable,$MySqlExecutable,$JarPath)){
  if(!(Test-Path -LiteralPath $file -PathType Leaf)){throw "No existe el archivo requerido: $file"}
  if($file -match '["\r\n]'){throw 'Ruta con caracteres no admitidos'}
}
if(!(Test-Path -LiteralPath $MySqlDataDirectory -PathType Container)){throw 'Falta la base aislada; este iniciador no inicializa ni borra bases'}
if($MySqlDataDirectory -match '["\r\n]'){throw 'Ruta de base no admitida'}
$configuration=Get-Content -LiteralPath $ConfigurationPath -Raw | ConvertFrom-Json
foreach($name in @('database','port','spring_password','migration_password','django_password','django_secret')){
  if(!$configuration.$name){throw "Falta configuración privada: $name"}
}
if($configuration.database -notmatch '^[a-zA-Z0-9_]+$'){throw 'Nombre de base no admitido'}
$logs=Join-Path $repository 'var/shared-architecture-logs'
New-Item -ItemType Directory -Force -Path $logs | Out-Null
function Get-ListenerProcess([int]$Port){
  $listener=Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1
  if($listener){return Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"}
}
$databaseProcess=Get-ListenerProcess ([int]$configuration.port)
if($databaseProcess){
  if($databaseProcess.CommandLine -notlike "*$MySqlDataDirectory*"){throw 'El puerto MySQL lo ocupa otra instancia; no se modifica'}
} else {
  Start-Process -FilePath $MySqlExecutable -ArgumentList @('--no-defaults',"--datadir=`"$MySqlDataDirectory`"","--port=$($configuration.port)",'--bind-address=127.0.0.1','--mysqlx=0','--console') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs 'mysql.stdout.log') -RedirectStandardError (Join-Path $logs 'mysql.stderr.log') | Out-Null
}
$env:MAIN_DB_URL="jdbc:mysql://127.0.0.1:$($configuration.port)/$($configuration.database)?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima&sessionVariables=time_zone=%27-05:00%27"
$env:MAIN_DB_USER='main_spring'
$env:MAIN_DB_PASSWORD=$configuration.spring_password
$env:SPRING_FLYWAY_USER='main_migration'
$env:SPRING_FLYWAY_PASSWORD=$configuration.migration_password
$env:SPRING_PROFILES_ACTIVE='mysql'
$env:MAIN_ADMIN_LEGACY_ENABLED='false'
$env:MAIN_DEMO_ENABLED=if($DemoData){'true'}else{'false'}
$env:SERVER_PORT='8088'
if($FfmpegDirectory){
  if(!(Test-Path -LiteralPath (Join-Path $FfmpegDirectory 'ffmpeg.exe') -PathType Leaf)){throw 'No existe FFmpeg en la ruta indicada'}
  $env:MAIN_MEDIA_FFMPEG_DIRECTORY=$FfmpegDirectory
}
$springProcess=Get-ListenerProcess 8088
if($springProcess){
  if($springProcess.CommandLine -notlike "*$JarPath*"){throw '8088 está ocupado por otro servidor; no se detiene'}
} else {
  Start-Process -FilePath $JavaExecutable -ArgumentList @('-jar',"`"$JarPath`"") -WorkingDirectory $repository -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs 'spring.stdout.log') -RedirectStandardError (Join-Path $logs 'spring.stderr.log') | Out-Null
}
$env:MAIN_DB_NAME=$configuration.database
$env:MAIN_DB_HOST='127.0.0.1'
$env:MAIN_DB_PORT=[string]$configuration.port
$env:MAIN_DB_USER='main_django'
$env:MAIN_DB_PASSWORD=$configuration.django_password
$env:MAIN_DJANGO_SECRET_KEY=$configuration.django_secret
$djangoProcess=Get-ListenerProcess 8090
if($djangoProcess){
  if($djangoProcess.CommandLine -notlike '*main_admin.wsgi:application*'){throw '8090 está ocupado por otro servidor; no se detiene'}
} else {
  Start-Process -FilePath $PythonExecutable -ArgumentList @('-m','waitress','--listen=127.0.0.1:8090','--threads=4','main_admin.wsgi:application') -WorkingDirectory (Join-Path $repository 'admin-django') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs 'django.stdout.log') -RedirectStandardError (Join-Path $logs 'django.stderr.log') | Out-Null
}
Write-Host 'MAIN: usuarios http://127.0.0.1:8088/ y administración http://127.0.0.1:8090/admin.html'
Write-Host 'Los procesos continúan en segundo plano. Verificar su arranque en var/shared-architecture-logs.'
