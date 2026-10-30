param(
 [Parameter(Mandatory=$true)][string]$ConfigurationPath,
 [Parameter(Mandatory=$true)][string]$JavaExecutable,
 [Parameter(Mandatory=$true)][string]$PythonExecutable,
 [Parameter(Mandatory=$true)][string]$CaddyExecutable
)
$ErrorActionPreference='Stop'
$repo=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$jar=Join-Path $repo 'target/main-sprint1-0.1.0.jar'
foreach($file in @($ConfigurationPath,$JavaExecutable,$PythonExecutable,$CaddyExecutable,$jar)){
 if(!(Test-Path -LiteralPath $file -PathType Leaf) -or $file -match '["\r\n]'){throw 'Ruta requerida inválida'}
}
function Test-LabPort([int]$Port){
 $client=[Net.Sockets.TcpClient]::new()
 try{return $client.ConnectAsync('127.0.0.1',$Port).Wait(350) -and $client.Connected}catch{return $false}finally{$client.Dispose()}
}
foreach($port in @(8188,8190,8443)){
 if(Test-LabPort $port){throw "Puerto $port ocupado. No se detiene el proceso existente."}
}
$config=Get-Content -LiteralPath $ConfigurationPath -Raw | ConvertFrom-Json
if($config.database -notmatch '^[a-zA-Z0-9_]+$' -or !$config.spring_password -or !$config.django_password -or !$config.django_secret){throw 'Configuración local incompleta'}
$logs=Join-Path $repo 'var/https-lab';New-Item -ItemType Directory -Force $logs | Out-Null
$previous=@{};$started=@()
function Set-LabEnv([string]$Name,[string]$Value){
 if(!$previous.ContainsKey($Name)){$previous[$Name]=[Environment]::GetEnvironmentVariable($Name,'Process')}
 [Environment]::SetEnvironmentVariable($Name,$Value,'Process')
}
function Wait-LabPort([int]$Port){
 $until=[DateTime]::UtcNow.AddSeconds(55)
 do{
  if(Test-LabPort $Port){return}
  Start-Sleep -Milliseconds 300
 }while([DateTime]::UtcNow -lt $until)
 throw "No abrió el puerto de prueba $Port; revisar logs locales."
}
try{
 Set-LabEnv 'SPRING_PROFILES_ACTIVE' 'mysql'
 Set-LabEnv 'MAIN_DB_URL' "jdbc:mysql://127.0.0.1:$($config.port)/$($config.database)?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima&sessionVariables=time_zone=%27-05:00%27"
 Set-LabEnv 'MAIN_DB_USER' 'main_spring';Set-LabEnv 'MAIN_DB_PASSWORD' $config.spring_password
 Set-LabEnv 'SPRING_FLYWAY_ENABLED' 'false' # existing isolated DB must already be migrated
 Set-LabEnv 'MAIN_DEMO_ENABLED' 'false';Set-LabEnv 'MAIN_ADMIN_LEGACY_ENABLED' 'false'
 Set-LabEnv 'SERVER_ADDRESS' '127.0.0.1';Set-LabEnv 'SERVER_PORT' '8188'
 Set-LabEnv 'SERVER_SERVLET_SESSION_COOKIE_SECURE' 'true'
 Set-LabEnv 'SERVER_SERVLET_SESSION_COOKIE_HTTP_ONLY' 'true'
 Set-LabEnv 'SERVER_SERVLET_SESSION_COOKIE_SAME_SITE' 'lax'
 Set-LabEnv 'SERVER_FORWARD_HEADERS_STRATEGY' 'native'
 Set-LabEnv 'SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES' '127\.0\.0\.1|::1|0:0:0:0:0:0:0:1'
 $started+=Start-Process $JavaExecutable -ArgumentList @('-jar',"`"$jar`"") -WorkingDirectory $repo -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logs 'spring.out.log') -RedirectStandardError (Join-Path $logs 'spring.err.log')
 Wait-LabPort 8188
 Set-LabEnv 'MAIN_DB_NAME' $config.database;Set-LabEnv 'MAIN_DB_HOST' '127.0.0.1';Set-LabEnv 'MAIN_DB_PORT' ([string]$config.port)
 Set-LabEnv 'MAIN_DB_USER' 'main_django';Set-LabEnv 'MAIN_DB_PASSWORD' $config.django_password
 Set-LabEnv 'MAIN_DJANGO_SECRET_KEY' $config.django_secret
 Set-LabEnv 'DJANGO_SETTINGS_MODULE' 'main_admin.production_settings'
 Set-LabEnv 'MAIN_PUBLIC_ORIGIN' 'https://localhost:8443'
 $started+=Start-Process $PythonExecutable -ArgumentList @('-m','waitress','--listen=127.0.0.1:8190','--threads=4','--trusted-proxy=127.0.0.1','--trusted-proxy-headers=x-forwarded-proto','main_admin.wsgi:application') -WorkingDirectory (Join-Path $repo 'admin-django') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logs 'django.out.log') -RedirectStandardError (Join-Path $logs 'django.err.log')
 Wait-LabPort 8190
 Set-LabEnv 'MAIN_CADDY_DATA_DIR' ((Join-Path $repo 'var/caddy').Replace('\','/'))
 $started+=Start-Process $CaddyExecutable -ArgumentList @('run','--config','deploy/Caddyfile.local','--adapter','caddyfile') -WorkingDirectory $repo -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logs 'caddy.out.log') -RedirectStandardError (Join-Path $logs 'caddy.err.log')
 Wait-LabPort 8443
 $started | Select-Object Id,ProcessName | ConvertTo-Json | Set-Content (Join-Path $logs 'processes.json')
 Write-Output 'Laboratorio HTTPS listo en https://localhost:8443. CA privada en var/caddy/pki/authorities/local/root.crt. No instalada en Windows.'
}catch{
 # Stop only processes this invocation created; never services belonging to the user.
 foreach($process in $started){if(!$process.HasExited){Stop-Process -Id $process.Id -ErrorAction SilentlyContinue}}
 throw
}finally{
 foreach($name in $previous.Keys){[Environment]::SetEnvironmentVariable($name,$previous[$name],'Process')}
}
