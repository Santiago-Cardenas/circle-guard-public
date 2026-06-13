# Valores propios del ambiente dev.
# Cuota aumentada para acomodar los sidecars de Istio (+200m CPU y +128Mi RAM por pod).
namespace_name = "circleguard-dev"
cpu_quota      = "20"
memory_quota   = "32Gi"
pods_quota     = "50"
