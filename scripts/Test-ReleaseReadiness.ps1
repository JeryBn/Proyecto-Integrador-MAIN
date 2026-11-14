[CmdletBinding()]
param(
    [string]$RepositoryRoot,
    [switch]$Json,
    [switch]$AllowIncomplete
)

$ErrorActionPreference = 'Stop'
$checks = [System.Collections.Generic.List[object]]::new()

if ([string]::IsNullOrWhiteSpace($RepositoryRoot)) {
    if ([string]::IsNullOrWhiteSpace($PSScriptRoot)) {
        throw 'No se pudo determinar la carpeta del verificador. Indica -RepositoryRoot.'
    }
    $RepositoryRoot = Split-Path -Parent $PSScriptRoot
}

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

function Get-ConfiguredEnvironment {
    param([string]$Name)
    $value = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if ([string]::IsNullOrWhiteSpace($value)) {
        $value = [Environment]::GetEnvironmentVariable($Name, 'User')
    }
    return $value
}

function Test-LeafFile {
    param([string]$Path)
    if (-not (Test-ConfiguredValue $Path)) { return $false }
    try { return Test-Path -LiteralPath $Path -PathType Leaf }
    catch { return $false }
}

function Read-PrivateJsonFile {
    param([string]$Path)
    if (-not (Test-LeafFile $Path)) { return $null }
    try {
        $file = Get-Item -LiteralPath $Path -Force
        if ($file.Length -le 0 -or $file.Length -gt 1MB) { return $null }
        return [IO.File]::ReadAllText($file.FullName) | ConvertFrom-Json -ErrorAction Stop
    } catch {
        return $null
    }
}

function Get-FirebaseAndroidReadiness {
    param(
        [string]$Path,
        [string]$PackageName
    )
    if (-not (Test-LeafFile $Path)) {
        return [pscustomobject]@{ ready = $false; detail = 'Falta mobile/app/google-services.json fuera de Git.' }
    }
    $configuration = Read-PrivateJsonFile $Path
    if ($null -eq $configuration) {
        return [pscustomobject]@{ ready = $false; detail = 'google-services.json no contiene un JSON válido de hasta 1 MiB.' }
    }
    $projectId = [string]$configuration.project_info.project_id
    $projectNumber = [string]$configuration.project_info.project_number
    $matchingClient = $false
    foreach ($client in @($configuration.client)) {
        $clientPackage = [string]$client.client_info.android_client_info.package_name
        $appId = [string]$client.client_info.mobilesdk_app_id
        $hasApiKey = $false
        foreach ($apiKey in @($client.api_key)) {
            if (Test-ConfiguredValue ([string]$apiKey.current_key)) { $hasApiKey = $true; break }
        }
        if ($clientPackage -eq $PackageName -and $appId -match '^1:[0-9]+:android:[A-Za-z0-9]+$' -and $hasApiKey) {
            $matchingClient = $true
            break
        }
    }
    $validProject = $projectId -match '^[a-z][a-z0-9-]{4,28}[a-z0-9]$' -and $projectNumber -match '^[0-9]{6,20}$'
    if (-not $validProject -or -not $matchingClient) {
        return [pscustomobject]@{ ready = $false; detail = "google-services.json no coincide con $PackageName o está incompleto." }
    }
    return [pscustomobject]@{ ready = $true; detail = "Configuración Firebase válida para $PackageName." }
}

function Get-FcmServerReadiness {
    param(
        [bool]$PushEnabled,
        [string]$Path
    )
    if (-not $PushEnabled) {
        return [pscustomobject]@{ ready = $false; detail = 'Falta MAIN_PUSH_ENABLED=true para la entrega.' }
    }
    if (-not (Test-ConfiguredValue $Path)) {
        return [pscustomobject]@{ ready = $false; detail = 'Falta la ruta privada MAIN_FCM_CREDENTIALS_FILE.' }
    }
    try {
        if (-not [IO.Path]::IsPathRooted($Path)) {
            return [pscustomobject]@{ ready = $false; detail = 'MAIN_FCM_CREDENTIALS_FILE debe ser una ruta privada absoluta.' }
        }
    } catch {
        return [pscustomobject]@{ ready = $false; detail = 'MAIN_FCM_CREDENTIALS_FILE no contiene una ruta válida.' }
    }
    if (-not (Test-LeafFile $Path)) {
        return [pscustomobject]@{ ready = $false; detail = 'No se encontró el archivo privado indicado por MAIN_FCM_CREDENTIALS_FILE.' }
    }
    $account = Read-PrivateJsonFile $Path
    if ($null -eq $account) {
        return [pscustomobject]@{ ready = $false; detail = 'La cuenta de servicio FCM no contiene un JSON válido de hasta 1 MiB.' }
    }
    $projectId = [string]$account.project_id
    $email = [string]$account.client_email
    $privateKeyId = [string]$account.private_key_id
    $privateKey = [string]$account.private_key
    $keyBody = $privateKey.Replace('-----BEGIN PRIVATE KEY-----', '').Replace('-----END PRIVATE KEY-----', '') -replace '\s', ''
    $valid = [string]$account.type -eq 'service_account' -and
        $projectId -match '^[a-z][a-z0-9-]{4,62}$' -and
        $email -match '^[^@\s]+@[^@\s]+\.gserviceaccount\.com$' -and
        $privateKeyId -match '^[a-fA-F0-9]{20,64}$' -and
        $privateKey.Contains('-----BEGIN PRIVATE KEY-----') -and
        $privateKey.Contains('-----END PRIVATE KEY-----') -and
        $keyBody.Length -ge 128 -and $keyBody -match '^[A-Za-z0-9+/=]+$' -and
        [string]$account.token_uri -eq 'https://oauth2.googleapis.com/token'
    if (-not $valid) {
        return [pscustomobject]@{ ready = $false; detail = 'La cuenta de servicio FCM no tiene la estructura privada esperada.' }
    }
    return [pscustomobject]@{ ready = $true; detail = 'Push habilitado y cuenta de servicio FCM estructuralmente válida.' }
}

$repositoryPath = (Resolve-Path -LiteralPath $RepositoryRoot).Path
$mobilePath = Join-Path $repositoryPath 'mobile'
$androidAppPath = Join-Path $mobilePath 'app'

$gitCommand = Get-Command git -ErrorAction SilentlyContinue
if ($null -eq $gitCommand -or -not (Test-Path -LiteralPath (Join-Path $repositoryPath '.git'))) {
    Add-ReadinessCheck 'Repositorio Git' $false $(if ($null -eq $gitCommand) { 'Git no está disponible.' } else { 'La carpeta indicada no es un repositorio Git.' })
} else {
    $status = @(& git -C $repositoryPath status --porcelain 2>$null)
    $commit = (& git -C $repositoryPath rev-parse --short HEAD 2>$null)
    $gitReady = $LASTEXITCODE -eq 0 -and $status.Count -eq 0
    Add-ReadinessCheck 'Repositorio Git' $gitReady $(if ($gitReady) { "Limpio en $commit." } else { 'Hay cambios sin confirmar o no se pudo leer el repositorio.' })
}

$publicOrigin = Get-ConfiguredEnvironment 'MAIN_PUBLIC_ORIGIN'
$originUri = [Uri]$null
$validOrigin = [Uri]::TryCreate([string]$publicOrigin, [UriKind]::Absolute, [ref]$originUri)
$originReady = $validOrigin -and $originUri.Scheme -eq 'https' -and
    [string]::IsNullOrEmpty($originUri.UserInfo) -and $originUri.AbsolutePath -eq '/' -and
    $originUri.Host -notin @('localhost', '127.0.0.1', '::1')
Add-ReadinessCheck 'Origen HTTPS público' $originReady $(if ($originReady) { 'Origen HTTPS exacto configurado.' } else { 'Falta MAIN_PUBLIC_ORIGIN con el dominio HTTPS definitivo.' })

$sdkDirectory = Get-ConfiguredEnvironment 'ANDROID_SDK_ROOT'
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
$firebaseStatus = Get-FirebaseAndroidReadiness $googleServices 'pe.tecsup.main.mobile'
Add-ReadinessCheck 'Firebase Android' $firebaseStatus.ready $firebaseStatus.detail

$signingNames = @(
    'MAIN_SIGNING_STORE_FILE',
    'MAIN_SIGNING_STORE_PASSWORD',
    'MAIN_SIGNING_KEY_ALIAS',
    'MAIN_SIGNING_KEY_PASSWORD'
)
$signingValues = @{}
foreach ($name in $signingNames) { $signingValues[$name] = Get-ConfiguredEnvironment $name }
$storeFile = $signingValues['MAIN_SIGNING_STORE_FILE']
if (Test-ConfiguredValue $storeFile -and -not [IO.Path]::IsPathRooted($storeFile)) {
    $storeFile = Join-Path $androidAppPath $storeFile
}
$signingReady = ($signingNames | Where-Object { -not (Test-ConfiguredValue $signingValues[$_]) }).Count -eq 0 -and
    (Test-Path -LiteralPath $storeFile -PathType Leaf)
Add-ReadinessCheck 'Firma Android' $signingReady $(if ($signingReady) { 'Las cuatro variables privadas y el archivo de firma están presentes.' } else { 'Falta el keystore original o alguna variable MAIN_SIGNING_*.' })

$fcmCredentials = Get-ConfiguredEnvironment 'MAIN_FCM_CREDENTIALS_FILE'
$pushEnabled = [string](Get-ConfiguredEnvironment 'MAIN_PUSH_ENABLED') -eq 'true'
$fcmStatus = Get-FcmServerReadiness $pushEnabled $fcmCredentials
Add-ReadinessCheck 'FCM del servidor' $fcmStatus.ready $fcmStatus.detail

$pushKey = Get-ConfiguredEnvironment 'MAIN_PUSH_ENCRYPTION_KEY'
$pushKeyReady = $false
if (Test-ConfiguredValue $pushKey) {
    try { $pushKeyReady = [Convert]::FromBase64String($pushKey).Length -eq 32 } catch { $pushKeyReady = $false }
}
Add-ReadinessCheck 'Cifrado de tokens push' $pushKeyReady $(if ($pushKeyReady) { 'Clave Base64 de 32 bytes configurada.' } else { 'Falta MAIN_PUSH_ENCRYPTION_KEY con 32 bytes en Base64.' })

$aiReady = Test-ConfiguredValue (Get-ConfiguredEnvironment 'OPENAI_API_KEY')
Add-ReadinessCheck 'Asistente IA' $aiReady $(if ($aiReady) { 'Credencial configurada; aún debe comprobarse cuota y respuesta real.' } else { 'Falta OPENAI_API_KEY y habilitar cuota del proyecto.' })

$ffmpegReady = $false
$ffmpegDirectory = Get-ConfiguredEnvironment 'MAIN_MEDIA_FFMPEG_DIRECTORY'
if (Test-ConfiguredValue $ffmpegDirectory) {
    $ffmpegReady = (Test-Path -LiteralPath (Join-Path $ffmpegDirectory 'ffmpeg.exe')) -or
        (Test-Path -LiteralPath (Join-Path $ffmpegDirectory 'ffmpeg'))
}
if (-not $ffmpegReady) { $ffmpegReady = $null -ne (Get-Command ffmpeg -ErrorAction SilentlyContinue) }
Add-ReadinessCheck 'Procesamiento multimedia' $ffmpegReady $(if ($ffmpegReady) { 'FFmpeg disponible.' } else { 'Falta MAIN_MEDIA_FFMPEG_DIRECTORY o FFmpeg en PATH.' })

$databaseVariables = @('MAIN_DB_HOST', 'MAIN_DB_NAME', 'MAIN_DB_USER', 'MAIN_DB_PASSWORD')
$databaseReady = ($databaseVariables | Where-Object { -not (Test-ConfiguredValue (Get-ConfiguredEnvironment $_)) }).Count -eq 0
$dbHostValue = Get-ConfiguredEnvironment 'MAIN_DB_HOST'
$remoteDatabase = $databaseReady -and $dbHostValue.ToLowerInvariant() -notin @('localhost', '127.0.0.1', '::1')
$databaseCa = Get-ConfiguredEnvironment 'MAIN_DB_SSL_CA'
$databaseCaReady = -not $remoteDatabase -or ((Test-ConfiguredValue $databaseCa) -and (Test-Path -LiteralPath $databaseCa -PathType Leaf))
Add-ReadinessCheck 'Base de datos de producción' ($databaseReady -and $databaseCaReady) $(if (-not $databaseReady) { 'Falta una o más variables MAIN_DB_* de producción.' } elseif (-not $databaseCaReady) { 'La base remota exige MAIN_DB_SSL_CA.' } else { 'Configuración presente; falta comprobar conexión y restauración.' })

$djangoSecret = Get-ConfiguredEnvironment 'MAIN_DJANGO_SECRET_KEY'
$djangoSecretReady = (Test-ConfiguredValue $djangoSecret) -and $djangoSecret.Length -ge 32
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

if (-not $summary.ready -and -not $AllowIncomplete) { exit 2 }
