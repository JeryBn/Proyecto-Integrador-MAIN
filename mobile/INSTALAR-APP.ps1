param(
    [string]$Device = '',
    [ValidateRange(1, 65535)][int]$BackendPort = 8088
)
$ErrorActionPreference = 'Stop'
$mobileRoot = $PSScriptRoot
$sdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdkRoot 'platform-tools\adb.exe'
$apk = Join-Path $mobileRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (!(Test-Path -LiteralPath $adb)) { throw 'No se encontró adb. Configura ANDROID_HOME con tu SDK Android.' }
if (!(Test-Path -LiteralPath $apk)) { throw 'Compila la app antes: gradlew.bat assembleDebug.' }
$devices = @(& $adb devices | Select-String '^([^\s]+)\s+device$' | ForEach-Object { $_.Matches[0].Groups[1].Value })
if (!$Device) {
    if ($devices.Count -ne 1) { throw 'Conecta un único dispositivo o usa -Device con su identificador.' }
    $Device = $devices[0]
}
if ($Device -notin $devices) { throw 'El dispositivo no está conectado o su depuración aún no está autorizada.' }
& $adb -s $Device reverse tcp:8088 "tcp:$BackendPort"
if ($LASTEXITCODE) { throw 'No se pudo conectar el puente local.' }
& $adb -s $Device install -r $apk
if ($LASTEXITCODE) { throw 'No se pudo instalar la aplicación.' }
& $adb -s $Device shell am start -n pe.tecsup.main.mobile/.MainActivity
if ($LASTEXITCODE) { throw 'No se pudo abrir la aplicación.' }
Write-Output "MAIN instalada. En la app usa http://127.0.0.1:8088. El puente apunta al puerto $BackendPort de esta PC."
