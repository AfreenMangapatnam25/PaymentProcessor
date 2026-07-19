# ---- build stage ----
FROM gradle:8.9-jdk17 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY src ./src
RUN gradle --no-daemon clean bootJar

# ---- runtime stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system clearing && useradd --system --gid clearing clearing
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN mkdir -p /app/clearing-files && chown -R clearing:clearing /app
USER clearing
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
