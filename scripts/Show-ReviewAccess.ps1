param([string]$VaultDirectory=(Join-Path $env:LOCALAPPDATA 'MAIN-private\reviewers'),[switch]$ValidateOnly)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
$accounts=@(Get-ChildItem -LiteralPath $VaultDirectory -Filter '*.review-access.json' | ForEach-Object {Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json})
if(!$accounts.Count){throw 'Todavía no hay accesos de revisión preparados.'}
$form=New-Object Windows.Forms.Form;$form.Text='Accesos de revisión MAIN';$form.Size=New-Object Drawing.Size(680,440);$form.StartPosition='CenterScreen'
$label=New-Object Windows.Forms.Label;$label.Text='Selecciona el titular. La clave temporal debe cambiarse en el primer ingreso.';$label.SetBounds(20,15,620,45)
$choice=New-Object Windows.Forms.ComboBox;$choice.DropDownStyle='DropDownList';$choice.SetBounds(20,65,620,30)
foreach($account in $accounts){[void]$choice.Items.Add($account.name)}
$username=New-Object Windows.Forms.TextBox;$username.ReadOnly=$true;$username.SetBounds(20,110,620,30)
$secret=New-Object Windows.Forms.TextBox;$secret.ReadOnly=$true;$secret.UseSystemPasswordChar=$true;$secret.SetBounds(20,155,620,30)
$show=New-Object Windows.Forms.CheckBox;$show.Text='Mostrar clave temporal en esta pantalla';$show.SetBounds(20,200,580,30)
$copy=New-Object Windows.Forms.Button;$copy.Text='Copiar clave temporal';$copy.SetBounds(20,245,280,35)
$info=New-Object Windows.Forms.Label;$info.Text='Acceso local: http://127.0.0.1:8090/admin.html. La copia se limpia al cerrar o tras un minuto si sigue en el portapapeles. Entrega a cada titular únicamente su propio acceso.';$info.SetBounds(20,295,620,75)
$state=@{copied=$null;copiedAt=[DateTime]::MinValue}
$choice.Add_SelectedIndexChanged({
 $account=$accounts[$choice.SelectedIndex];$username.Text=$account.username;$secret.Text=''
 $secure=ConvertTo-SecureString $account.protectedPassword;$pointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
 try{$secret.Text=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)}finally{[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer);$secure.Dispose()}
})
$show.Add_CheckedChanged({$secret.UseSystemPasswordChar=!$show.Checked})
$copy.Add_Click({[Windows.Forms.Clipboard]::SetText($secret.Text);$state.copied=$secret.Text;$state.copiedAt=[DateTime]::UtcNow})
$timer=New-Object Windows.Forms.Timer;$timer.Interval=1000
$timer.Add_Tick({if($state.copied -and [DateTime]::UtcNow.Subtract($state.copiedAt).TotalSeconds -ge 60){if([Windows.Forms.Clipboard]::GetText() -ceq $state.copied){[Windows.Forms.Clipboard]::Clear()};$state.copied=$null}})
$form.Controls.AddRange(@($label,$choice,$username,$secret,$show,$copy,$info))
if($ValidateOnly){$timer.Dispose();$form.Dispose();Write-Output 'Interfaz de accesos cargada; no se mostró ni descifró ninguna clave.';return}
$choice.SelectedIndex=0;$timer.Start()
try{[void]$form.ShowDialog()}finally{$timer.Stop();$timer.Dispose();if($state.copied -and [Windows.Forms.Clipboard]::GetText() -ceq $state.copied){[Windows.Forms.Clipboard]::Clear()};$secret.Text='';$state.copied=$null;$form.Dispose()}
