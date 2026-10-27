param([switch]$CrearFirma)
$ErrorActionPreference = 'Stop'
if ($env:OS -ne 'Windows_NT') { throw 'La protección privada requiere Windows DPAPI.' }
$privateDirectory = Join-Path $env:LOCALAPPDATA 'MAIN-private\android'
$configurationPath = Join-Path $privateDirectory 'signing.json'
$keytool = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\keytool.exe' } else { 'keytool.exe' }
if ($CrearFirma) {
    if (Test-Path -LiteralPath $privateDirectory) { throw 'Ya existe la carpeta privada. No se sobrescribe ninguna firma.' }
    New-Item -ItemType Directory -Path $privateDirectory | Out-Null
    $identity = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
    & icacls.exe $privateDirectory /inheritance:r /grant:r "${identity}:(OI)(CI)F" | Out-Null
    if ($LASTEXITCODE) { throw 'No se pudo restringir el acceso a la carpeta privada.' }
    $random = New-Object byte[] 48
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($random) } finally { $generator.Dispose() }
    $password = [Convert]::ToBase64String($random)
    $env:MAIN_TEMP_SIGNING_PASSWORD = $password
    $store = Join-Path $privateDirectory 'main-release-2026.p12'
    $certificate = Join-Path $privateDirectory 'main-release-2026.cer'
    try {
        & $keytool -genkeypair -noprompt -storetype PKCS12 -keystore $store -alias main-release-2026 -keyalg RSA -keysize 3072 -validity 9125 -dname 'CN=MAIN Android Release, OU=Proyecto MAIN' -storepass:env MAIN_TEMP_SIGNING_PASSWORD -keypass:env MAIN_TEMP_SIGNING_PASSWORD
        if ($LASTEXITCODE) { throw 'No se pudo crear la firma. Conserva la carpeta privada para revisar el fallo.' }
        & $keytool -exportcert -keystore $store -alias main-release-2026 -file $certificate -storepass:env MAIN_TEMP_SIGNING_PASSWORD
        if ($LASTEXITCODE) { throw 'No se pudo exportar el certificado público.' }
        $protected = ConvertFrom-SecureString (ConvertTo-SecureString $password -AsPlainText -Force)
        [ordered]@{ store=$store; alias='main-release-2026'; protectedPassword=$protected; certificateSha256=(Get-FileHash $certificate -Algorithm SHA256).Hash.ToLowerInvariant() } | ConvertTo-Json | Set-Content -LiteralPath $configurationPath -Encoding UTF8
        Write-Output 'Firma creada fuera de Git. Contraseña protegida con DPAPI para esta cuenta de Windows.'
    } finally { Remove-Item Env:MAIN_TEMP_SIGNING_PASSWORD -ErrorAction SilentlyContinue; $password=$null }
    return
}
if (!(Test-Path -LiteralPath $configurationPath)) { throw 'Primero crea la firma con -CrearFirma.' }
$configuration = Get-Content -LiteralPath $configurationPath -Raw | ConvertFrom-Json
$secure = ConvertTo-SecureString $configuration.protectedPassword
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
$names = 'MAIN_SIGNING_STORE_FILE','MAIN_SIGNING_STORE_PASSWORD','MAIN_SIGNING_KEY_ALIAS','MAIN_SIGNING_KEY_PASSWORD'
$previous = @{}
foreach ($name in $names) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    $env:MAIN_SIGNING_STORE_FILE = $configuration.store
    $env:MAIN_SIGNING_KEY_ALIAS = $configuration.alias
    $env:MAIN_SIGNING_STORE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    $env:MAIN_SIGNING_KEY_PASSWORD = $env:MAIN_SIGNING_STORE_PASSWORD
    & (Join-Path $PSScriptRoot 'PREPARAR-ENTREGA.ps1') -ExpectedCertificateSha256 $configuration.certificateSha256
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    $secure.Dispose()
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
}
