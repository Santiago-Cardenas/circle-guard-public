# Build de imágenes Docker para los 6 microservicios del Taller 2.
# Uso:
#   PowerShell:  ./infra/docker/build-all.ps1 [-Tag dev]
#   Bash:        TAG=dev ./infra/docker/build-all.sh

param(
  [string]$Tag = "dev"
)

$services = @(
  @{ Name = "circleguard-identity-service";     Image = "circleguard/identity" },
  @{ Name = "circleguard-auth-service";         Image = "circleguard/auth" },
  @{ Name = "circleguard-gateway-service";      Image = "circleguard/gateway" },
  @{ Name = "circleguard-promotion-service";    Image = "circleguard/promotion" },
  @{ Name = "circleguard-notification-service"; Image = "circleguard/notification" },
  @{ Name = "circleguard-dashboard-service";    Image = "circleguard/dashboard" }
)

$ErrorActionPreference = "Continue"
$PSNativeCommandUseErrorActionPreference = $false
$root = Resolve-Path "$PSScriptRoot/../.."

foreach ($s in $services) {
  Write-Host "==> Building $($s.Image):$Tag" -ForegroundColor Cyan
  docker build `
    -f "$root/infra/docker/service.Dockerfile" `
    --build-arg SERVICE_NAME=$($s.Name) `
    -t "$($s.Image):$Tag" `
    "$root" 2>&1 | Out-Host
  if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed for $($s.Name) (exit=$LASTEXITCODE)" -ForegroundColor Red
    exit 1
  }
}

Write-Host "`nAll images built:" -ForegroundColor Green
docker images --filter "reference=circleguard/*:$Tag" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
