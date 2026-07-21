# Build context is the repo root (see docker-compose.yml: build.context: .). The whole repo is
# copied in (not just the sibling modules this service depends on) because the root pom.xml's
# <modules> reactor lists every module, including the legacy monolith (core/app/infra/web/
# bootstrap) — Maven fails to even parse the reactor if any listed module directory is missing,
# regardless of -pl/-am scoping which modules actually get BUILT.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl conceptarena-kernel,jwt-validation-lib,auth-service -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/auth-service/target/auth-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
