param([Parameter(Mandatory = $true)][string]$ExpectedCertificateSha256)
$ErrorActionPreference = 'Stop'
# Passwords are read only by Gradle from the process environment. Never print them.
$required = 'MAIN_SIGNING_STORE_FILE', 'MAIN_SIGNING_STORE_PASSWORD', 'MAIN_SIGNING_KEY_ALIAS', 'MAIN_SIGNING_KEY_PASSWORD'
foreach ($name in $required) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Falta configurar $name en esta sesión privada."
    }
}
if (!(Test-Path -LiteralPath $env:MAIN_SIGNING_STORE_FILE -PathType Leaf)) { throw 'No existe el archivo de firma indicado.' }
$expected = ($ExpectedCertificateSha256 -replace ':|\s', '').ToLowerInvariant()
if ($expected -notmatch '^[0-9a-f]{64}$') { throw 'Indica la huella SHA-256 completa del certificado de entrega aprobado.' }
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$signer = Join-Path $sdk 'build-tools\36.0.0\apksigner.bat'
$analyzer = Join-Path $sdk 'build-tools\36.0.0\aapt.exe'
if (!(Test-Path -LiteralPath $signer) -or !(Test-Path -LiteralPath $analyzer)) { throw 'Instala Android SDK Build Tools 36.0.0 o configura ANDROID_HOME.' }
Push-Location $PSScriptRoot
try {
    $revision = & git rev-parse HEAD
    if ($LASTEXITCODE) { throw 'No se pudo obtener el commit.' }
    $dirty = & git status --porcelain
    if ($LASTEXITCODE -or $dirty) { throw 'La entrega necesita un árbol Git limpio y un commit identificable.' }
    # Clean prevents accidentally distributing an APK from an earlier build.
    & .\gradlew.bat --no-daemon --console=plain clean assembleRelease
    if ($LASTEXITCODE) { throw 'Falló la compilación de entrega.' }
    $apk = Join-Path $PSScriptRoot 'app\build\outputs\apk\release\app-release.apk'
    if (!(Test-Path -LiteralPath $apk)) { throw 'No se generó el APK firmado esperado.' }
    $verification = @(& $signer verify --verbose --print-certs $apk 2>&1)
    if ($LASTEXITCODE) { throw 'La verificación criptográfica del APK falló.' }
    $digest = @($verification | Select-String '^Signer #1 certificate SHA-256 digest: ([0-9a-fA-F]+)$')
    if ($digest.Count -ne 1 -or $digest[0].Matches[0].Groups[1].Value.ToLowerInvariant() -ne $expected) {
        throw 'El certificado del APK no coincide con el certificado de entrega aprobado.'
    }
    $badging = @(& $analyzer dump badging $apk)
    if ($LASTEXITCODE) { throw 'No se pudo inspeccionar la identidad del APK.' }
    if ($badging -match '^application-debuggable') { throw 'El APK es depurable; no es una entrega válida.' }
    $package = @($badging | Select-String "^package: name='pe.tecsup.main.mobile' versionCode='([^']+)' versionName='([^']+)'")
    if ($package.Count -ne 1) { throw 'Identidad del paquete inesperada.' }
    $manifest = [ordered]@{
        commit = "$revision".Trim(); generatedAtUtc = [DateTime]::UtcNow.ToString('o')
        applicationId = 'pe.tecsup.main.mobile'
        versionCode = $package[0].Matches[0].Groups[1].Value
        versionName = $package[0].Matches[0].Groups[2].Value
        apk = 'app-release.apk'; sha256 = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
        certificateSha256 = $expected; signatureVerified = $true
        physicalPhoneAcceptance = 'PENDIENTE'; httpsEnvironmentAcceptance = 'PENDIENTE'
    }
    $manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path (Split-Path $apk) 'entrega.json') -Encoding UTF8
    Write-Output 'APK y entrega.json disponibles en app/build/outputs/apk/release. Firma verificada; aceptación funcional pendiente.'
} finally { Pop-Location }
