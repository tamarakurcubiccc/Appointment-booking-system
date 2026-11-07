# ---------------------------------------------------------------------------
# One parameterised Dockerfile for every module.
# Build with:  docker build --build-arg MODULE=user-service -t user-service .
# The build context is the repository root because this is a Maven multi-module
# reactor build (each service depends on the `common` module).
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

ARG MODULE
WORKDIR /workspace

# Copy only the POMs first, so the (slow) dependency download is cached
# and only re-runs when a pom.xml actually changes.
COPY pom.xml ./
COPY common/pom.xml common/
COPY discovery-server/pom.xml discovery-server/
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/
COPY catalog-service/pom.xml catalog-service/
COPY reservation-service/pom.xml reservation-service/
COPY notification-service/pom.xml notification-service/
COPY loyalty-service/pom.xml loyalty-service/

# `|| true`: go-offline cannot always resolve intra-reactor modules; the real
# build below fills any gaps. This step exists purely to warm the layer cache.
RUN mvn -B -pl ${MODULE} -am -DskipTests dependency:go-offline || true

COPY . .
RUN mvn -B -pl ${MODULE} -am -DskipTests package \
 && cp ${MODULE}/target/*.jar /workspace/app.jar

# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

# curl is used by the Docker Compose health checks
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system spring \
 && useradd --system --gid spring spring

WORKDIR /app

COPY --from=build --chown=spring:spring /workspace/app.jar app.jar

USER spring
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
