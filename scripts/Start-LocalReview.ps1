param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$reviewRepo = Split-Path $PSScriptRoot -Parent
$reviewJar = Join-Path $reviewRepo 'target/main-sprint1-0.1.0.jar'
if (!(Test-Path -LiteralPath $reviewJar)) { throw 'Primero compila el servidor con Maven verify.' }
if (!$JavaHome -or !(Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) { throw 'Configura JAVA_HOME con JDK 21.' }
if (Get-NetTCPConnection -LocalPort 8088 -State Listen -ErrorAction SilentlyContinue) { throw 'Ya hay un servidor en 8088. Comprueba su origen; no se detuvo ningún proceso.' }
$reviewRuntime = Join-Path $reviewRepo 'var/review-runtime'
New-Item -ItemType Directory -Force -Path $reviewRuntime | Out-Null
Copy-Item -LiteralPath $reviewJar -Destination (Join-Path $reviewRuntime 'main-api.jar') -Force
$reviewConfig = (Join-Path $reviewRepo '.env.local').Replace('\','/')
$reviewArgs = @('-jar', ('"'+(Join-Path $reviewRuntime 'main-api.jar')+'"'),
 '--spring.datasource.url=jdbc:h2:file:./main-review;DB_CLOSE_ON_EXIT=FALSE',
 '--main.demo.enabled=true',
 '--main.admin.username=android_test_admin',
 '--main.admin.password=SyntheticAndroidTest2026!',
 ('--spring.config.import=optional:file:'+$reviewConfig+'[.properties]'))
$reviewProcess = Start-Process -FilePath (Join-Path $JavaHome 'bin/java.exe') -ArgumentList $reviewArgs -WorkingDirectory $reviewRuntime -WindowStyle Hidden -RedirectStandardOutput (Join-Path $reviewRuntime 'server.stdout.log') -RedirectStandardError (Join-Path $reviewRuntime 'server.stderr.log') -PassThru
$reviewProcess.Id | Set-Content -LiteralPath (Join-Path $reviewRuntime 'server.pid')
Write-Output ('Servidor de revisión iniciado, PID '+$reviewProcess.Id+'. Espera el arranque y abre http://127.0.0.1:8088.')
Write-Output 'Solo datos sintéticos: alumno, otro o vacio / MainDemo2026!. Administrador de pruebas: android_test_admin / SyntheticAndroidTest2026!.'
