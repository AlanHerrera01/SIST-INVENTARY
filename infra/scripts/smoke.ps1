param(
    [int]$FrontendPort = 5173,
    [int]$BackendPort = 8080,
    [int]$KeycloakPort = 8081,
    [int]$KeycloakManagementPort = 9000
)
$ErrorActionPreference = 'Stop'
$checks = @(
    @{ Name = 'Frontend'; Url = "http://localhost:$FrontendPort/" },
    @{ Name = 'Backend liveness'; Url = "http://localhost:$BackendPort/actuator/health/liveness" },
    @{ Name = 'Backend readiness (MySQL + OIDC)'; Url = "http://localhost:$BackendPort/actuator/health/readiness" },
    @{ Name = 'Keycloak readiness'; Url = "http://localhost:$KeycloakManagementPort/health/ready" },
    @{ Name = 'OIDC discovery'; Url = "http://localhost:$KeycloakPort/realms/inventory-dev/.well-known/openid-configuration" },
    @{ Name = 'Proxy frontend -> backend'; Url = "http://localhost:$FrontendPort/actuator/health/readiness" }
)
foreach ($check in $checks) {
    $response = Invoke-WebRequest -UseBasicParsing -TimeoutSec 15 -Uri $check.Url
    if ($response.StatusCode -ne 200) { throw "Failed: $($check.Name)" }
    if ($check.Name -match 'liveness|readiness|Proxy') {
        if (($response.Content | ConvertFrom-Json).status -ne 'UP') { throw "Not UP: $($check.Name)" }
    }
    if ($check.Name -eq 'OIDC discovery') {
        $metadata = $response.Content | ConvertFrom-Json
        if ($metadata.issuer -ne "http://localhost:$KeycloakPort/realms/inventory-dev") { throw 'Unexpected issuer' }
    }
    Write-Host "OK: $($check.Name)"
}
