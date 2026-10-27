param(
 [Parameter(Mandatory=$true)][string]$ConfigurationPath,
 [Parameter(Mandatory=$true)][string]$PythonExecutable,
 [switch]$Apply,
 [switch]$Background,
 [ValidateRange(60,86400)][int]$IntervalSeconds=3600
)
$ErrorActionPreference='Stop'
$repository=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$configuration=Get-Content -LiteralPath $ConfigurationPath -Raw | ConvertFrom-Json
if (!$configuration.retention_protected_password) { throw 'Falta la cuenta privada dedicada de retención (contraseña DPAPI).' }
if (!(Test-Path -LiteralPath $PythonExecutable -PathType Leaf)) { throw 'No existe Python.' }
$logs=Join-Path $repository 'var/retention'
New-Item -ItemType Directory -Force -Path $logs | Out-Null
$pidFile=Join-Path $logs 'worker.pid'
if ($Background -and (Test-Path $pidFile)) {
 $workerId=0
 if ([int]::TryParse((Get-Content $pidFile -Raw).Trim(),[ref]$workerId)) {
  $worker=Get-CimInstance Win32_Process -Filter "ProcessId=$workerId"
  if ($worker -and $worker.CommandLine -like '*retain_content*') { throw 'El proceso de retención ya está ejecutándose.' }
 }
}
$names='MAIN_DB_NAME','MAIN_DB_HOST','MAIN_DB_PORT','MAIN_DB_USER','MAIN_DB_PASSWORD','MAIN_DJANGO_SECRET_KEY','MAIN_RETENTION_APPLY'
$previous=@{}
foreach($name in $names){$previous[$name]=[Environment]::GetEnvironmentVariable($name,'Process')}
$secure=ConvertTo-SecureString $configuration.retention_protected_password
$pointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
 $env:MAIN_DB_NAME=$configuration.database
 $env:MAIN_DB_HOST='127.0.0.1'
 $env:MAIN_DB_PORT=[string]$configuration.port
 $env:MAIN_DB_USER='main_retention'
 $env:MAIN_DB_PASSWORD=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
 $env:MAIN_DJANGO_SECRET_KEY=$configuration.django_secret
 $env:MAIN_RETENTION_APPLY=if($Apply){'jery-2026-10-27-v1'}else{''}
 $arguments=@('manage.py','retain_content','--limit','100')
 if($Apply){$arguments+='--apply'}
 if($Background){
  $arguments+=@('--watch','--interval',[string]$IntervalSeconds)
  $process=Start-Process -FilePath $PythonExecutable -ArgumentList $arguments -WorkingDirectory (Join-Path $repository 'admin-django') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs 'worker.stdout.log') -RedirectStandardError (Join-Path $logs 'worker.stderr.log') -PassThru
  $process.Id | Set-Content $pidFile
  Write-Output 'Proceso de retención iniciado; revisar var/retention para confirmar cada lote. No se modifica el inicio de Windows.'
 }else{
  Push-Location (Join-Path $repository 'admin-django')
  try { & $PythonExecutable @arguments; if($LASTEXITCODE){throw 'Falló la retención; revisar permisos y configuración.'} } finally {Pop-Location}
 }
}finally{
 [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer);$secure.Dispose()
 foreach($name in $names){[Environment]::SetEnvironmentVariable($name,$previous[$name],'Process')}
}
