provider "azurerm" {
  features {}
  skip_provider_registration = true
  # Autenticar con: az login
  # O credenciales de service principal via variables de entorno:
  # ARM_CLIENT_ID, ARM_CLIENT_SECRET, ARM_SUBSCRIPTION_ID, ARM_TENANT_ID
}
