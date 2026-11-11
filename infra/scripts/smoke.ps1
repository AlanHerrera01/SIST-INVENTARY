param(
    [int]$FrontendPort = 5173,
    [int]$BackendPort = 8080,
    [int]$KeycloakPort = 8081,
    [int]$KeycloakManagementPort = 9000
)
$ErrorActionPreference = 'Stop'

function ConvertTo-JsonBody($response) {
    $content = $response.Content
    # Windows PowerShell 5.1 returns byte[] for vendor JSON content types.
    if ($content -is [byte[]]) {
        $content = [System.Text.Encoding]::UTF8.GetString($content)
    }
    return $content | ConvertFrom-Json
}

$checks = @(
    @{ Name = 'Frontend'; Url = "http://localhost:$FrontendPort/"; Health = $false },
    @{ Name = 'Backend liveness'; Url = "http://localhost:$BackendPort/actuator/health/liveness"; Health = $true },
    @{ Name = 'Backend readiness (MySQL + OIDC)'; Url = "http://localhost:$BackendPort/actuator/health/readiness"; Health = $true },
    @{ Name = 'Keycloak readiness'; Url = "http://localhost:$KeycloakManagementPort/health/ready"; Health = $false },
    @{ Name = 'OIDC discovery'; Url = "http://localhost:$KeycloakPort/realms/inventory-dev/.well-known/openid-configuration"; Health = $false },
    @{ Name = 'Proxy frontend -> backend'; Url = "http://localhost:$FrontendPort/actuator/health/readiness"; Health = $true }
)

foreach ($check in $checks) {
    $response = Invoke-WebRequest -UseBasicParsing -TimeoutSec 15 -Uri $check.Url
    if ($response.StatusCode -ne 200) { throw "Failed: $($check.Name)" }
    if ($check.Health) {
        if ((ConvertTo-JsonBody $response).status -ne 'UP') { throw "Not UP: $($check.Name)" }
    }
    if ($check.Name -eq 'OIDC discovery') {
        $metadata = ConvertTo-JsonBody $response
        if ($metadata.issuer -ne "http://localhost:$KeycloakPort/realms/inventory-dev") { throw 'Unexpected issuer' }
    }
    Write-Host "OK: $($check.Name)"
}
