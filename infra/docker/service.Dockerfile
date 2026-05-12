# ----------------------------------------------------------------------
# Dockerfile compartido para los 6 microservicios CircleGuard.
# Build:  docker build -f infra/docker/service.Dockerfile \
#                      --build-arg SERVICE_NAME=circleguard-auth-service \
#                      -t circleguard/auth:dev .
# ----------------------------------------------------------------------

ARG JDK_IMAGE=eclipse-temurin:21-jdk-jammy
ARG JRE_IMAGE=eclipse-temurin:21-jre-jammy

# ---------- Stage 1: build ----------
FROM ${JDK_IMAGE} AS builder
ARG SERVICE_NAME
WORKDIR /workspace

# Copia mínima necesaria para resolver Gradle (cache friendly)
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

# Copiar todos los build.gradle.kts de los servicios (para que Gradle resuelva el grafo)
COPY services ./services

# Construir solo el servicio pedido (omitiendo tests; las pruebas corren en el pipeline)
RUN chmod +x ./gradlew \
 && ./gradlew --no-daemon :services:${SERVICE_NAME}:bootJar -x test

# Renombra el jar a un nombre fijo para la siguiente stage.
# Spring Boot bootJar genera DOS archivos: el boot jar ejecutable y un *-plain.jar.
# Se debe seleccionar exclusivamente el boot jar (excluyendo el plain).
RUN set -e \
 && JAR="$(find services/${SERVICE_NAME}/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)" \
 && test -n "$JAR" || (echo "ERROR: no se encontró bootJar en services/${SERVICE_NAME}/build/libs"; ls -la services/${SERVICE_NAME}/build/libs; exit 1) \
 && cp "$JAR" /workspace/app.jar \
 && ls -la /workspace/app.jar

# ---------- Stage 2: runtime ----------
FROM ${JRE_IMAGE} AS runtime
ARG SERVICE_NAME
ENV SERVICE_NAME=${SERVICE_NAME}
WORKDIR /app

# Usuario no-root
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
COPY --from=builder --chown=app:app /workspace/app.jar /app/app.jar
USER app

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health 2>/dev/null | grep -q UP || exit 1

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
