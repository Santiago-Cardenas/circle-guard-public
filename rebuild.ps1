$tags = 'dev','stage','prod'
$services = @(
  'circleguard-identity-service|circleguard/identity',
  'circleguard-auth-service|circleguard/auth',
  'circleguard-gateway-service|circleguard/gateway',
  'circleguard-promotion-service|circleguard/promotion',
  'circleguard-notification-service|circleguard/notification',
  'circleguard-dashboard-service|circleguard/dashboard'
)

foreach ($tag in $tags) {
  Write-Host "Building for $tag..." -ForegroundColor Green
  foreach ($entry in $services) {
    $svc, $img = $entry -split '\|'
    $jar = Get-ChildItem -Path "services/$svc/build/libs" -Filter '*.jar' | Where-Object {$_.Name -notmatch '-plain'} | Select-Object -First 1
    if ($jar) {
      docker build -f infra/docker/service.runtime.Dockerfile --build-arg SERVICE_NAME=$svc --build-arg JAR_PATH="services/$svc/build/libs/$($jar.Name)" -t "$img`:$tag" . | Out-Null
      Write-Host "  ✓ $img`:$tag"
    }
  }
}

Write-Host "Done!" -ForegroundColor Cyan
docker images circleguard/ | Select-Object Repository, Tag, Size
