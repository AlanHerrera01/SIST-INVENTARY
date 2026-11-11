param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$secretDirectory = Join-Path $projectRoot '.secrets'
New-Item -ItemType Directory -Force -Path $secretDirectory | Out-Null
$names = @('mysql_root_password', 'app_db_password', 'keycloak_db_password', 'keycloak_admin_password', 'oidc_client_secret', 'oidc_admin_client_secret')
$generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    foreach ($name in $names) {
        $secretPath = Join-Path $secretDirectory $name
        if (-not (Test-Path -LiteralPath $secretPath)) {
            $bytes = New-Object byte[] 32
            $generator.GetBytes($bytes)
            $value = ([BitConverter]::ToString($bytes)).Replace('-', '').ToLowerInvariant()
            # CreateNew protects existing credentials even with concurrent invocations.
            $stream = [System.IO.File]::Open($secretPath, [System.IO.FileMode]::CreateNew)
            try {
                $encoded = [System.Text.Encoding]::UTF8.GetBytes($value)
                $stream.Write($encoded, 0, $encoded.Length)
            } finally { $stream.Dispose() }
        }
    }
} finally { $generator.Dispose() }

if ($env:OS -eq 'Windows_NT') {
    $currentSid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    & icacls.exe $secretDirectory /inheritance:r /grant:r "*${currentSid}:(OI)(CI)F" '*S-1-5-18:(OI)(CI)F' /Q | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not restrict secret directory ACLs' }
    foreach ($name in $names) {
        & icacls.exe (Join-Path $secretDirectory $name) /inheritance:r /grant:r "*${currentSid}:F" '*S-1-5-18:F' /Q | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Could not restrict secret file ACLs' }
    }
} else {
    & chmod 700 $secretDirectory
    foreach ($name in $names) {
        & chmod 644 (Join-Path $secretDirectory $name)
    }
    if ($LASTEXITCODE -ne 0) { throw 'Could not restrict secret file permissions' }
}

$environmentPath = Join-Path $projectRoot '.env'
if (-not (Test-Path -LiteralPath $environmentPath)) {
    Copy-Item -LiteralPath (Join-Path $projectRoot '.env.example') -Destination $environmentPath
}
Write-Host 'Entorno local preparado. Los secretos existentes no fueron modificados.'
