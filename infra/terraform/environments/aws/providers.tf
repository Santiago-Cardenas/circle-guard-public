provider "aws" {
  region = var.region
  # Credenciales desde variables de entorno: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
  # O configure con: aws configure
}
