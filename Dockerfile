FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /workspace/build/libs/finflow-backend.jar app.jar

USER 10001
EXPOSE 8080

ENTRYPOINT [
    "java",
    "-XX:InitialRAMPercentage=20.0",
    "-XX:MaxRAMPercentage=65.0",
    "-XX:+UseG1GC",
    "-jar",
    "/app/app.jar"
]