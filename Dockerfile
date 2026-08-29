FROM eclipse-temurin:17-jre

WORKDIR /app
COPY build/libs/finflow-backend.jar app.jar

USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

