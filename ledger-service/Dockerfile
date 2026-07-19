# ---- Build stage ---------------------------------------------------------
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY src ./src
# Produce the runnable Spring Boot jar.
RUN gradle --no-daemon clean bootJar

# ---- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# curl for the container healthcheck; run as a non-root user.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system ledger && useradd --system --gid ledger ledger

COPY --from=build /workspace/build/libs/*.jar /app/ledger-service.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080
USER ledger

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/ledger-service.jar"]
