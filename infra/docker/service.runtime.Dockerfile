# ----------------------------------------------------------------------
# Dockerfile RUNTIME para los 6 microservicios CircleGuard.
#
# Diferencia con service.Dockerfile (legado): no compila el JAR aqui.
# El JAR ya viene compilado en el contexto (build/libs/), pasado por
# JAR_PATH como build-arg. Esto evita arrancar 6 JVMs Gradle consecutivas
# dentro de Docker Desktop, que ahogaban el daemon en WSL2 (free tier).
#
# Build:
#   docker build -f infra/docker/service.runtime.Dockerfile \
#       --build-arg JAR_PATH=services/circleguard-auth-service/build/libs/circleguard-auth-service-1.0.0-SNAPSHOT.jar \
#       --build-arg SERVICE_NAME=circleguard-auth-service \
#       -t circleguard/auth:dev .
# ----------------------------------------------------------------------

ARG JRE_IMAGE=eclipse-temurin:21-jre-jammy

FROM ${JRE_IMAGE}
ARG SERVICE_NAME
ARG JAR_PATH
ENV SERVICE_NAME=${SERVICE_NAME}
WORKDIR /app

# Usuario no-root
RUN groupadd --system app && useradd --system --gid app --home-dir /app app

# Copia el JAR ya compilado por Gradle en el host
COPY --chown=app:app ${JAR_PATH} /app/app.jar

USER app

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health 2>/dev/null | grep -q UP || exit 1

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
