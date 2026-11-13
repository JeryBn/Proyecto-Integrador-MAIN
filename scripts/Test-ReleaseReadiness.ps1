[CmdletBinding()]
param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot),
    [switch]$Json
)

$ErrorActionPreference = 'Stop'
$checks = [System.Collections.Generic.List[object]]::new()

function Add-ReadinessCheck {
    param(
        [string]$Name,
        [bool]$Ready,
        [string]$Detail,
        [bool]$Required = $true
    )
    $checks.Add([pscustomobject]@{
        check = $Name
        ready = $Ready
        required = $Required
        detail = $Detail
    })
}

function Test-ConfiguredValue {
    param([string]$Value)
    return -not [string]::IsNullOrWhiteSpace($Value) -and $Value -notmatch 'example|replace|changeme'
}

$repositoryPath = (Resolve-Path -LiteralPath $RepositoryRoot).Path
$mobilePath = Join-Path $repositoryPath 'mobile'
$androidAppPath = Join-Path $mobilePath 'app'

$gitCommand = Get-Command git -ErrorAction SilentlyContinue
if ($null -eq $gitCommand) {
    Add-ReadinessCheck 'Repositorio Git' $false 'Git no está disponible.'
} else {
    $status = @(& git -C $repositoryPath status --porcelain 2>$null)
    $commit = (& git -C $repositoryPath rev-parse --short HEAD 2>$null)
    $gitReady = $LASTEXITCODE -eq 0 -and $status.Count -eq 0
    Add-ReadinessCheck 'Repositorio Git' $gitReady $(if ($gitReady) { "Limpio en $commit." } else { 'Hay cambios sin confirmar o no se pudo leer el repositorio.' })
}

$publicOrigin = $env:MAIN_PUBLIC_ORIGIN
$originUri = [Uri]$null
$validOrigin = [Uri]::TryCreate([string]$publicOrigin, [UriKind]::Absolute, [ref]$originUri)
$originReady = $validOrigin -and $originUri.Scheme -eq 'https' -and
    [string]::IsNullOrEmpty($originUri.UserInfo) -and $originUri.AbsolutePath -eq '/' -and
    $originUri.Host -notin @('localhost', '127.0.0.1', '::1')
Add-ReadinessCheck 'Origen HTTPS público' $originReady $(if ($originReady) { 'Origen HTTPS exacto configurado.' } else { 'Falta MAIN_PUBLIC_ORIGIN con el dominio HTTPS definitivo.' })

$sdkDirectory = $env:ANDROID_SDK_ROOT
if ([string]::IsNullOrWhiteSpace($sdkDirectory)) {
    $localProperties = Join-Path $mobilePath 'local.properties'
    if (Test-Path -LiteralPath $localProperties) {
        $sdkLine = Get-Content -LiteralPath $localProperties | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($sdkLine) {
            $sdkDirectory = $sdkLine.Substring($sdkLine.IndexOf('=') + 1).Replace('\:', ':').Replace('\\', '\')
        }
    }
}
$adbPath = $null
if (Test-ConfiguredValue $sdkDirectory) {
    $candidate = Join-Path $sdkDirectory 'platform-tools\adb.exe'
    if (Test-Path -LiteralPath $candidate) { $adbPath = $candidate }
}
if ($null -eq $adbPath) {
    $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCommand) { $adbPath = $adbCommand.Source }
}
Add-ReadinessCheck 'Android SDK y ADB' ($null -ne $adbPath) $(if ($adbPath) { 'ADB disponible.' } else { 'No se encontró ADB en el SDK configurado.' })

$deviceCount = 0
if ($adbPath) {
    $deviceRows = @(& $adbPath devices 2>$null | Where-Object { $_ -match "\tdevice$" })
    $deviceCount = $deviceRows.Count
}
Add-ReadinessCheck 'Teléfono físico autorizado' ($deviceCount -gt 0) $(if ($deviceCount -gt 0) { "$deviceCount dispositivo(s) disponible(s)." } else { 'Conecta, desbloquea y autoriza un teléfono Android por USB.' })

$googleServices = Join-Path $androidAppPath 'google-services.json'
Add-ReadinessCheck 'Firebase Android' (Test-Path -LiteralPath $googleServices) $(if (Test-Path -LiteralPath $googleServices) { 'Configuración privada presente.' } else { 'Falta mobile/app/google-services.json fuera de Git.' })

$signingNames = @(
    'MAIN_SIGNING_STORE_FILE',
    'MAIN_SIGNING_STORE_PASSWORD',
    'MAIN_SIGNING_KEY_ALIAS',
    'MAIN_SIGNING_KEY_PASSWORD'
)
$signingValues = @{}
foreach ($name in $signingNames) { $signingValues[$name] = [Environment]::GetEnvironmentVariable($name) }
$storeFile = $signingValues['MAIN_SIGNING_STORE_FILE']
if (Test-ConfiguredValue $storeFile -and -not [IO.Path]::IsPathRooted($storeFile)) {
    $storeFile = Join-Path $androidAppPath $storeFile
}
$signingReady = ($signingNames | Where-Object { -not (Test-ConfiguredValue $signingValues[$_]) }).Count -eq 0 -and
    (Test-Path -LiteralPath $storeFile -PathType Leaf)
Add-ReadinessCheck 'Firma Android' $signingReady $(if ($signingReady) { 'Las cuatro variables privadas y el archivo de firma están presentes.' } else { 'Falta el keystore original o alguna variable MAIN_SIGNING_*.' })

$fcmCredentials = $env:MAIN_FCM_CREDENTIALS_FILE
$pushEnabled = $env:MAIN_PUSH_ENABLED -eq 'true'
$fcmReady = $pushEnabled -and (Test-ConfiguredValue $fcmCredentials) -and (Test-Path -LiteralPath $fcmCredentials -PathType Leaf)
Add-ReadinessCheck 'FCM del servidor' $fcmReady $(if ($fcmReady) { 'Push habilitado y cuenta de servicio privada presente.' } elseif (-not $pushEnabled) { 'Falta MAIN_PUSH_ENABLED=true para la entrega.' } else { 'Falta la ruta privada MAIN_FCM_CREDENTIALS_FILE.' })

$pushKeyReady = $false
if (Test-ConfiguredValue $env:MAIN_PUSH_ENCRYPTION_KEY) {
    try { $pushKeyReady = [Convert]::FromBase64String($env:MAIN_PUSH_ENCRYPTION_KEY).Length -eq 32 } catch { $pushKeyReady = $false }
}
Add-ReadinessCheck 'Cifrado de tokens push' $pushKeyReady $(if ($pushKeyReady) { 'Clave Base64 de 32 bytes configurada.' } else { 'Falta MAIN_PUSH_ENCRYPTION_KEY con 32 bytes en Base64.' })

$aiReady = Test-ConfiguredValue $env:OPENAI_API_KEY
Add-ReadinessCheck 'Asistente IA' $aiReady $(if ($aiReady) { 'Credencial configurada; aún debe comprobarse cuota y respuesta real.' } else { 'Falta OPENAI_API_KEY y habilitar cuota del proyecto.' })

$ffmpegReady = $false
$ffmpegDirectory = $env:MAIN_MEDIA_FFMPEG_DIRECTORY
if (Test-ConfiguredValue $ffmpegDirectory) {
    $ffmpegReady = (Test-Path -LiteralPath (Join-Path $ffmpegDirectory 'ffmpeg.exe')) -or
        (Test-Path -LiteralPath (Join-Path $ffmpegDirectory 'ffmpeg'))
}
if (-not $ffmpegReady) { $ffmpegReady = $null -ne (Get-Command ffmpeg -ErrorAction SilentlyContinue) }
Add-ReadinessCheck 'Procesamiento multimedia' $ffmpegReady $(if ($ffmpegReady) { 'FFmpeg disponible.' } else { 'Falta MAIN_MEDIA_FFMPEG_DIRECTORY o FFmpeg en PATH.' })

$databaseVariables = @('MAIN_DB_HOST', 'MAIN_DB_NAME', 'MAIN_DB_USER', 'MAIN_DB_PASSWORD')
$databaseReady = ($databaseVariables | Where-Object { -not (Test-ConfiguredValue ([Environment]::GetEnvironmentVariable($_))) }).Count -eq 0
$dbHostValue = $env:MAIN_DB_HOST
$remoteDatabase = $databaseReady -and $dbHostValue.ToLowerInvariant() -notin @('localhost', '127.0.0.1', '::1')
$databaseCaReady = -not $remoteDatabase -or ((Test-ConfiguredValue $env:MAIN_DB_SSL_CA) -and (Test-Path -LiteralPath $env:MAIN_DB_SSL_CA -PathType Leaf))
Add-ReadinessCheck 'Base de datos de producción' ($databaseReady -and $databaseCaReady) $(if (-not $databaseReady) { 'Falta una o más variables MAIN_DB_* de producción.' } elseif (-not $databaseCaReady) { 'La base remota exige MAIN_DB_SSL_CA.' } else { 'Configuración presente; falta comprobar conexión y restauración.' })

$djangoSecretReady = (Test-ConfiguredValue $env:MAIN_DJANGO_SECRET_KEY) -and $env:MAIN_DJANGO_SECRET_KEY.Length -ge 32
Add-ReadinessCheck 'Secreto Django' $djangoSecretReady $(if ($djangoSecretReady) { 'Secreto privado configurado.' } else { 'Falta MAIN_DJANGO_SECRET_KEY de al menos 32 caracteres.' })

$blocking = @($checks | Where-Object { $_.required -and -not $_.ready })
$summary = [pscustomobject]@{
    ready = $blocking.Count -eq 0
    checkedAt = [DateTimeOffset]::Now.ToString('o')
    blockers = $blocking.Count
    checks = $checks
}

if ($Json) {
    $summary | ConvertTo-Json -Depth 5
} else {
    $checks | Select-Object @{Name='Estado'; Expression={ if ($_.ready) { 'LISTO' } else { 'FALTA' } }}, check, detail | Format-Table -AutoSize -Wrap
    if ($summary.ready) { Write-Host 'La configuración de entrega está completa.' -ForegroundColor Green }
    else { Write-Host "$($blocking.Count) bloqueo(s) de entrega siguen pendientes." -ForegroundColor Yellow }
}

if (-not $summary.ready) { exit 2 }
