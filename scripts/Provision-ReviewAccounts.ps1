param(
 [Parameter(Mandatory=$true)][string]$ConfigurationPath,
 [Parameter(Mandatory=$true)][string]$PythonExecutable
)
$ErrorActionPreference='Stop'
$repository=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$python=(Resolve-Path -LiteralPath $PythonExecutable).Path
$configuration=Get-Content -LiteralPath $ConfigurationPath -Raw | ConvertFrom-Json
$vault=Join-Path $env:LOCALAPPDATA 'MAIN-private\reviewers'
New-Item -ItemType Directory -Force -Path $vault | Out-Null
if((Get-Item -LiteralPath $vault).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'La bóveda no puede ser un enlace.'}
$identity=[Security.Principal.WindowsIdentity]::GetCurrent().Name
& icacls.exe $vault /inheritance:r /grant:r "${identity}:(OI)(CI)F" | Out-Null
if($LASTEXITCODE){throw 'No se pudo limitar el acceso a la bóveda.'}
$names='MAIN_DB_NAME','MAIN_DB_HOST','MAIN_DB_PORT','MAIN_DB_USER','MAIN_DB_PASSWORD','MAIN_DJANGO_SECRET_KEY','MAIN_REVIEWER_INITIAL_PASSWORD'
$previous=@{};foreach($name in $names){$previous[$name]=[Environment]::GetEnvironmentVariable($name,'Process')}
try{
 $env:MAIN_DB_NAME=$configuration.database;$env:MAIN_DB_HOST='127.0.0.1';$env:MAIN_DB_PORT=[string]$configuration.port
 $env:MAIN_DB_USER='main_migration';$env:MAIN_DB_PASSWORD=$configuration.migration_password;$env:MAIN_DJANGO_SECRET_KEY=$configuration.django_secret
 Push-Location (Join-Path $repository 'admin-django')
 try{
  foreach($account in @(@{username='jery.revision';name='Jery Becerra Ninaquispe'},@{username='michael.revision';name='Michael Julian Fernandez Camarena'})){
   $inspection=& $python manage.py provision_reviewer --username $account.username --name $account.name --inspect
   if($LASTEXITCODE){throw 'No se pudo verificar la identidad existente; no se sobrescribe.'}
   $existing=$inspection | ConvertFrom-Json
   $target=Join-Path $vault ($account.username+'.review-access.json')
   if($existing.exists){
    if(!(Test-Path -LiteralPath $target)){throw 'Existe una cuenta sin su bóveda local. No se cambia su contraseña automáticamente.'}
    Write-Output ($account.username+': ya existía; no se cambió la contraseña ni la habilitación.')
    continue
   }
   if(Test-Path -LiteralPath $target){
    if((Get-Item -LiteralPath $target).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Archivo de bóveda no seguro.'}
    $entry=Get-Content -LiteralPath $target -Raw | ConvertFrom-Json
    if($entry.username -ne $account.username -or $entry.name -ne $account.name){throw 'La bóveda corresponde a otra identidad.'}
    $secure=ConvertTo-SecureString $entry.protectedPassword
    $pointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try{$env:MAIN_REVIEWER_INITIAL_PASSWORD=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)}finally{[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer);$secure.Dispose()}
   }else{
    $random=New-Object byte[] 48;$generator=[Security.Cryptography.RandomNumberGenerator]::Create()
    try{$generator.GetBytes($random)}finally{$generator.Dispose()}
    $env:MAIN_REVIEWER_INITIAL_PASSWORD=[Convert]::ToBase64String($random)
    $protected=ConvertFrom-SecureString (ConvertTo-SecureString $env:MAIN_REVIEWER_INITIAL_PASSWORD -AsPlainText -Force)
    [ordered]@{username=$account.username;name=$account.name;role='REVIEWER';url='http://127.0.0.1:8090/admin.html';protectedPassword=$protected;createdAtUtc=[DateTime]::UtcNow.ToString('o')} | ConvertTo-Json | Set-Content -LiteralPath $target -Encoding UTF8
   }
   & $python manage.py provision_reviewer --username $account.username --name $account.name
   if($LASTEXITCODE){throw 'No se pudo crear la cuenta; se conserva la bóveda protegida para recuperación, sin sobrescribir identidades.'}
   Remove-Item Env:MAIN_REVIEWER_INITIAL_PASSWORD -ErrorAction SilentlyContinue
  }
 }finally{Pop-Location}
 Write-Output 'Claves temporales protegidas con DPAPI fuera de Git; cada titular debe configurar su contraseña personal.'
}finally{foreach($name in $names){[Environment]::SetEnvironmentVariable($name,$previous[$name],'Process')}}
