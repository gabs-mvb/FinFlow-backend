# syntax=docker/dockerfile:1
FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY core/build.gradle.kts ./core/build.gradle.kts
COPY core/src ./core/src
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/build/libs/finflow-backend.jar app.jar

USER 10001:10001
EXPOSE 8080

ENTRYPOINT ["java", "-XX:InitialRAMPercentage=20.0", "-XX:MaxRAMPercentage=65.0", "-XX:+UseG1GC", "-jar", "/app/app.jar"]
