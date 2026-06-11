#!/usr/bin/env bash
# Manda un correo simple a Mailhog para avisar cuando un pipeline falla.
# Uso: bash pipelines/notify.sh "asunto" "cuerpo del mensaje"
#
# Mailhog corre dentro de Kubernetes y lo exponemos por NodePort (puerto 30025),
# por eso desde Jenkins lo alcanzamos en host.docker.internal:30025.
set -e

SUBJECT="$1"
BODY="$2"
SMTP="${MAILHOG_SMTP:-host.docker.internal:30025}"
FROM="jenkins@circleguard.local"
TO="equipo@circleguard.local"

# Armamos el correo en un archivo temporal con el formato que pide SMTP.
MSG="$(mktemp)"
cat > "${MSG}" <<EOF
From: ${FROM}
To: ${TO}
Subject: ${SUBJECT}

${BODY}
EOF

# curl sabe hablar SMTP, asi no dependemos de plugins extra de Jenkins.
curl --silent --show-error --url "smtp://${SMTP}" \
  --mail-from "${FROM}" \
  --mail-rcpt "${TO}" \
  --upload-file "${MSG}"

rm -f "${MSG}"
echo "Notificacion enviada a Mailhog (${SMTP})."
