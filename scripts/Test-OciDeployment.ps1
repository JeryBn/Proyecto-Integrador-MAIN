[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repository = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$deployment = Join-Path $repository 'deploy/oci'
$failures = [Collections.Generic.List[string]]::new()

function Require-File([string]$RelativePath) {
    $path = Join-Path $repository $RelativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $failures.Add("Falta $RelativePath")
        return $null
    }
    return (Get-Content -LiteralPath $path -Raw)
}

function Require-Pattern([string]$Text, [string]$Pattern, [string]$Message) {
    if ($null -eq $Text -or $Text -notmatch $Pattern) { $failures.Add($Message) }
}

function Forbid-Pattern([string]$Text, [string]$Pattern, [string]$Message) {
    if ($null -ne $Text -and $Text -match $Pattern) { $failures.Add($Message) }
}

$compose = Require-File 'deploy/oci/compose.yaml'
$prepare = Require-File 'deploy/oci/prepare.sh'
$deploy = Require-File 'deploy/oci/deploy.sh'
$backup = Require-File 'deploy/oci/backup.sh'
$springImage = Require-File 'deploy/oci/spring.Dockerfile'
$djangoImage = Require-File 'deploy/oci/django.Dockerfile'
$caddy = Require-File 'deploy/Caddyfile'
$production = Require-File 'admin-django/main_admin/production_settings.py'
$ignore = Require-File '.gitignore'

foreach ($required in @(
    'deploy/oci/cloud-init.yaml',
    'deploy/oci/mysql/init-users.sh',
    'deploy/oci/mysql/apply-grants.sh',
    'deploy/oci/systemd/main-backup.service',
    'deploy/oci/systemd/main-backup.timer',
    'deploy/oci/README.md'
)) { [void](Require-File $required) }

Require-Pattern $compose '(?m)^\s+internal:\s+true\s*$' 'La red de base de datos no está marcada como interna.'
Require-Pattern $compose 'require-secure-transport=ON' 'MySQL no exige transporte cifrado.'
Require-Pattern $compose 'MAIN_MYSQL_CA_FILE' 'Spring no recibe la CA privada de MySQL.'
Require-Pattern $compose 'MAIN_DB_SSL_CA' 'Django no recibe la CA privada de MySQL.'
Require-Pattern $compose '(?m)^\s+restart:\s+unless-stopped\s*$' 'Falta recuperación automática de servicios persistentes.'
Require-Pattern $compose '(?m)^\s+healthcheck:\s*$' 'Faltan comprobaciones de salud de contenedores.'
Require-Pattern $compose 'no-new-privileges:true' 'Falta el límite no-new-privileges.'
Require-Pattern $compose '(?m)^\s+- 80:80\s*$' 'Caddy no publica HTTP para el desafío y la redirección HTTPS.'
Require-Pattern $compose '(?m)^\s+- 443:443\s*$' 'Caddy no publica HTTPS.'
Forbid-Pattern $compose '(?m)^\s+- (3306|8088|8090):' 'Un servicio interno publica su puerto en el host.'
Forbid-Pattern $compose '(?m)image:\s*[^\r\n]+:latest\s*$' 'Hay una imagen latest no fijada.'

Require-Pattern $prepare 'umask 077' 'La generación de secretos no fija permisos privados.'
Require-Pattern $prepare 'no se sobrescribe' 'La preparación no documenta su rechazo a sobrescribir secretos.'
Require-Pattern $deploy 'docker compose up --detach --remove-orphans --wait' 'El despliegue no espera salud integral.'
Require-Pattern $backup 'sha256sum' 'Los respaldos no generan un manifiesto SHA-256.'
Require-Pattern $backup 'gzip --test' 'Los respaldos no se comprueban antes de publicarse.'

Require-Pattern $springImage '(?m)^FROM node:[^\r\n]+ AS web-build' 'La imagen Spring no compila el frontend de forma reproducible.'
Require-Pattern $springImage '(?m)^FROM maven:[^\r\n]+ AS java-build' 'La imagen Spring no tiene construcción Maven separada.'
Require-Pattern $djangoImage 'USER 10001:10001' 'Django no se ejecuta con usuario sin privilegios.'
Require-Pattern $caddy 'MAIN_DJANGO_UPSTREAM' 'Caddy no permite upstream privado para Django.'
Require-Pattern $production "'ssl_mode': 'VERIFY_IDENTITY'" 'Django producción no verifica el hostname de MySQL.'

foreach ($entry in @('deploy/oci/.env', 'deploy/oci/secrets/', 'deploy/oci/backups/', 'deploy/oci/initial-access.txt')) {
    if ($ignore -notmatch [regex]::Escape($entry)) { $failures.Add(".gitignore no protege $entry") }
}

$trackedPrivate = & git -C $repository ls-files -- 'deploy/oci/.env' 'deploy/oci/secrets/*' 'deploy/oci/backups/*' 'deploy/oci/initial-access.txt'
if ($LASTEXITCODE -ne 0) { throw 'No se pudo inspeccionar el índice Git.' }
if ($trackedPrivate) { $failures.Add('Hay archivos privados de OCI versionados en Git.') }

if ($failures.Count) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Host 'Despliegue OCI validado: red privada, TLS, secretos, salud y respaldos presentes.' -ForegroundColor Green
