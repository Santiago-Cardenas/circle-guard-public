#!/usr/bin/env pwsh
$Dockerfile = 'infra/docker/service.runtime.Dockerfile'
$services = @(
  @{svc='circleguard-identity-service'; img='circleguard/identity'},
  @{svc='circleguard-auth-service'; img='circleguard/auth'},
  @{svc='circleguard-gateway-service'; img='circleguard/gateway'},
  @{svc='circleguard-promotion-service'; img='circleguard/promotion'},
  @{svc='circleguard-notification-service'; img='circleguard/notification'},
  @{svc='circleguard-dashboard-service'; img='circleguard/dashboard'}
)

$tags = @('dev','stage','prod')

foreach ($tag in $tags) {
  Write-Host "`n===== Building TAG=$tag =====" -ForegroundColor Green
  foreach ($service in $services) {
    $svcName = $service.svc
    $imgName = $service.img
    $jarPath = Get-ChildItem -Path "services/$svcName/build/libs" -Filter '*.jar' -Exclude '*-plain.jar' | Select-Object -First 1
    if ($jarPath) {
      $relPath = "services/$svcName/build/libs/$($jarPath.Name)"
      Write-Host "Building $imgName`:$tag"
      docker build -f $Dockerfile --build-arg SERVICE_NAME=$svcName --build-arg JAR_PATH=$relPath -t "$imgName`:$tag" . | Out-Null
      Write-Host "✓ $imgName`:$tag" -ForegroundColor Green
    } else {
      Write-Host "✗ No JAR: $svcName" -ForegroundColor Red
    }
  }
}

Write-Host "`nDone. Current images:" -ForegroundColor Cyan
docker images circleguard/ --format '{{.Repository}}:{{.Tag}}'
