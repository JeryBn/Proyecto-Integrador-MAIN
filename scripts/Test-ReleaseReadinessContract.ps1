[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$readinessScript = Join-Path $PSScriptRoot 'Test-ReleaseReadiness.ps1'
$temporaryRoot = Join-Path ([IO.Path]::GetTempPath()) ('main-readiness-' + [Guid]::NewGuid().ToString('N'))
$googleServices = Join-Path $temporaryRoot 'mobile\app\google-services.json'
$fcmCredentials = Join-Path $temporaryRoot 'private\firebase-admin.json'
$environmentNames = @('MAIN_PUSH_ENABLED', 'MAIN_FCM_CREDENTIALS_FILE')
$originalEnvironment = @{}
$assertions = 0

function Write-Utf8Json {
    param([string]$Path, [object]$Value)
    $directory = Split-Path -Parent $Path
    [IO.Directory]::CreateDirectory($directory) | Out-Null
    $json = $Value | ConvertTo-Json -Depth 10
    [IO.File]::WriteAllText($Path, $json, [Text.UTF8Encoding]::new($false))
}

function Invoke-Readiness {
    $output = & $readinessScript -RepositoryRoot $temporaryRoot -Json -AllowIncomplete
    return $output | ConvertFrom-Json
}

function Get-NamedCheck {
    param([object]$Summary, [string]$Name)
    $match = @($Summary.checks | Where-Object { $_.check -eq $Name })
    if ($match.Count -ne 1) { throw "No se encontró exactamente una comprobación '$Name'." }
    return $match[0]
}

function Assert-Ready {
    param([object]$Summary, [string]$Name, [bool]$Expected)
    $script:assertions++
    $actual = [bool](Get-NamedCheck $Summary $Name).ready
    if ($actual -ne $Expected) { throw "'$Name' devolvió ready=$actual; se esperaba $Expected." }
}

function New-FirebaseConfiguration {
    param([string]$PackageName)
    return [ordered]@{
        project_info = [ordered]@{ project_number = '123456789012'; project_id = 'main-project' }
        client = @([ordered]@{
            client_info = [ordered]@{
                mobilesdk_app_id = '1:123456789012:android:abcdef123456'
                android_client_info = [ordered]@{ package_name = $PackageName }
            }
            api_key = @([ordered]@{ current_key = 'synthetic-contract-key' })
        })
        configuration_version = '1'
    }
}

try {
    [IO.Directory]::CreateDirectory((Split-Path -Parent $googleServices)) | Out-Null
    foreach ($name in $environmentNames) {
        $originalEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
    }
    [Environment]::SetEnvironmentVariable('MAIN_PUSH_ENABLED', 'true', 'Process')
    [Environment]::SetEnvironmentVariable('MAIN_FCM_CREDENTIALS_FILE', $fcmCredentials, 'Process')

    Assert-Ready (Invoke-Readiness) 'Firebase Android' $false
    [IO.File]::WriteAllText($googleServices, '{invalid', [Text.UTF8Encoding]::new($false))
    Assert-Ready (Invoke-Readiness) 'Firebase Android' $false
    Write-Utf8Json $googleServices (New-FirebaseConfiguration 'pe.tecsup.main.wrong')
    Assert-Ready (Invoke-Readiness) 'Firebase Android' $false
    Write-Utf8Json $googleServices (New-FirebaseConfiguration 'pe.tecsup.main.mobile')
    Assert-Ready (Invoke-Readiness) 'Firebase Android' $true

    [IO.Directory]::CreateDirectory((Split-Path -Parent $fcmCredentials)) | Out-Null
    [IO.File]::WriteAllText($fcmCredentials, '{invalid', [Text.UTF8Encoding]::new($false))
    Assert-Ready (Invoke-Readiness) 'FCM del servidor' $false
    Write-Utf8Json $fcmCredentials ([ordered]@{ type = 'authorized_user'; project_id = 'main-project' })
    Assert-Ready (Invoke-Readiness) 'FCM del servidor' $false
    Write-Utf8Json $fcmCredentials ([ordered]@{
        type = 'service_account'
        project_id = 'main-project'
        private_key_id = ('a' * 40)
        private_key = "-----BEGIN PRIVATE KEY-----`n$(('A' * 160))`n-----END PRIVATE KEY-----`n"
        client_email = 'firebase-adminsdk-test@main-project.iam.gserviceaccount.com'
        token_uri = 'https://oauth2.googleapis.com/token'
    })
    Assert-Ready (Invoke-Readiness) 'FCM del servidor' $true
    [Environment]::SetEnvironmentVariable('MAIN_PUSH_ENABLED', 'false', 'Process')
    Assert-Ready (Invoke-Readiness) 'FCM del servidor' $false

    Write-Host "$assertions comprobaciones del contrato de preflight aprobadas."
} finally {
    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable($name, $originalEnvironment[$name], 'Process')
    }
    if (Test-Path -LiteralPath $temporaryRoot) {
        Remove-Item -LiteralPath $temporaryRoot -Recurse -Force
    }
}
