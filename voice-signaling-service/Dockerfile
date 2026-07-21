# Build context is the repo root (see docker-compose.yml: build.context: .). The whole repo is
# copied in because the root pom.xml's <modules> reactor lists every module (including the
# legacy monolith) — Maven fails to parse the reactor if any listed module directory is missing.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl jwt-validation-lib,voice-signaling-service -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/voice-signaling-service/target/voice-signaling-service-1.0.0.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
