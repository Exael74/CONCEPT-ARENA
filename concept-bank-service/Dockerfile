# Build context is the repo root (see docker-compose.yml: build.context: .). The whole repo is
# copied in because the root pom.xml's <modules> reactor lists every module (including the
# legacy monolith) — Maven fails to parse the reactor if any listed module directory is missing.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl conceptarena-kernel,jwt-validation-lib,concept-bank-service -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/concept-bank-service/target/concept-bank-service-1.0.0.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
